import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('publisher', Path(__file__).resolve().parents[1] / 'tools/publish_verified_release.py')
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)
SHA = 'a' * 40


def workflow(name, **changes):
    result = dict(id=1, path=f'.github/workflows/{name}.yml', status='completed',
                  conclusion='success', head_sha=SHA, head_branch='main', event='push')
    result.update(changes)
    return result


def successful_runs():
    return [workflow(name) for name in ['android', 'ios', 'web', 'pages']]


class ReleaseGateTest(unittest.TestCase):
    def check_main_blocked(self, dirty='', config='RequestsOpenAccess: false', remote=SHA, expected=''):
        def fake_run(*args):
            if args[:3] == ('git', 'status', '--porcelain'): return dirty
            if args[:3] == ('git', 'rev-parse', 'HEAD'): return SHA
            if args[:2] == ('git', 'ls-remote'): return remote + '\trefs/heads/main'
            raise AssertionError(f'Unexpected release mutation: {args}')
        with patch.object(publisher, 'run', fake_run), patch.object(Path, 'read_text', return_value=config):
            with self.assertRaisesRegex(SystemExit, expected): publisher.main()

    def check_runs_blocked(self, runs, expected):
        with patch.object(publisher, 'run', return_value=json.dumps({'workflow_runs': runs})):
            with self.assertRaisesRegex(SystemExit, expected): publisher.verified_runs(SHA)

    def test_dirty_checkout(self):
        self.check_main_blocked(dirty=' M file', expected='dirty checkout')

    def test_unapproved_capability_change_blocks_release(self):
        self.check_main_blocked(config='RequestsOpenAccess: true', expected='Full Access disabled')

    def test_unpushed_source_blocks_release(self):
        self.check_main_blocked(remote='b' * 40, expected='final pushed main SHA')

    def test_missing_workflow(self):
        self.check_runs_blocked([], 'android.yml has not succeeded')

    def test_latest_failed_run_cannot_use_older_success(self):
        self.check_runs_blocked(successful_runs() + [workflow('android', id=2, conclusion='failure')], 'android.yml has not succeeded')

    def test_each_required_platform_must_succeed(self):
        for index, name in enumerate(['android', 'ios', 'web', 'pages']):
            with self.subTest(name=name):
                runs = successful_runs()
                runs[index]['conclusion'] = 'failure'
                self.check_runs_blocked(runs, f'{name}.yml has not succeeded')

    def test_queued_or_running_is_not_success(self):
        for status in ['queued', 'in_progress']:
            runs = successful_runs()
            runs[1]['status'] = status
            self.check_runs_blocked(runs, 'ios.yml has not succeeded')

    def test_stale_sha_is_rejected(self):
        runs = successful_runs()
        runs[1]['head_sha'] = 'b' * 40
        self.check_runs_blocked(runs, 'ios.yml has not succeeded')

    def test_pull_request_or_other_branch_is_rejected(self):
        for change in [dict(event='pull_request'), dict(head_branch='other')]:
            runs = successful_runs()
            runs[1].update(change)
            self.check_runs_blocked(runs, 'ios.yml has not succeeded')

    def test_successful_final_sha_and_dispatch_are_accepted(self):
        runs = successful_runs()
        runs[1]['event'] = 'workflow_dispatch'
        with patch.object(publisher, 'run', return_value=json.dumps({'workflow_runs': runs})):
            self.assertEqual(len(publisher.verified_runs(SHA)), 4)

    def test_release_target_is_v03(self):
        self.assertEqual(publisher.VERSION, '0.3')


if __name__ == '__main__': unittest.main()
