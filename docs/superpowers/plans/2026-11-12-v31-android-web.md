# DevBoard 3.1: Android ir žiniatinklio simuliatoriaus darbų specifikacija

Data: 2026-11-12

## Apimtis

Įgyvendinti ankstesnį 0.3 pakeitimų planą kaip naują vartotojo nurodytą 3.1 versiją: Android klaviatūros ir jos internetinio simuliatoriaus funkcijas. Pirmiausia ištirti, kas jau veikia, o tada taisyti tik aptiktus neatitikimus. iOS šiuo metu nedaroma ir jos failai nekeičiami. Android ir simuliatoriaus darbas savaime nepatvirtina jokio iOS funkcijų įgyvendinimo.

Dirbama atskirame darbo medyje, esami main failai ir paskelbtas leidimas nekeičiami. Dokumente aprašyti tikslai, ne teiginiai, kad funkcijos jau išbandytos.

## Funkcijos ir priėmimo kriterijai

### 1. Shift ir Caps Lock

- Vienas Shift paspaudimas taikomas vienai raidei; ją įvedus grįžtama į mažąsias, nebent aktyvus Caps Lock arba iš tikrųjų taikoma sakinio pradžios automatinė didžioji.
- Dvigubas arba ilgas Shift paspaudimas įjungia nuolatinį didžiųjų raidžių režimą; naudotojas gali jį išjungti. Kito klavišo paspaudimas neturi klaidingai užbaigti seno dvigubo paspaudimo.
- PC išdėstyme Shift turi keisti ir jam numatytus skaičių bei skyrybos ženklus, o Caps Lock neturi keisti simbolių.
- Patikrinti: vienkartinį Shift, abu Caps Lock įjungimo būdus, išjungimą, automatinę didžiąją po taško ir tarpo, vaizdą bei įvedamus simbolius.

### 2. Space ir kalbos pasirinkimas

- Viename Space klaviše rodyti kairįjį ženklą, centre Space pavadinimą ir dešinįjį ženklą vienoje horizontalioje linijoje: < Space >. Abu krašto ženklai matomi ir siaurame ekrane.
- Ženklai yra dekoratyviniai: bakstelėjimas bet kurioje klavišo vietoje įveda vieną tarpą. Ilgas paspaudimas atidaro kalbos pasirinkimą, neįterpdamas netyčinio simbolio.
- Patikrinti kairįjį kraštą, centrą ir dešinįjį kraštą, ilgą paspaudimą, kalbos keitimą, siaurą ekraną ir didesnį šriftą.

### 3. Taško ir kablelio meniu

- Ilgai spaudžiant tašką arba kablelį atidaromas atitinkamų papildomų ženklų pasirinkimas. Meniu turi būti pakeltas virš klavišo maždaug per vieno klavišo aukštį, kad jo neuždengtų pirštas.
- Pasirinkus ženklą įterpiamas pasirinktas simbolis; nutraukus veiksmą meniu uždaromas be nepageidaujamo įvedimo.
- Patikrinti abu klavišus, tikrą pasirinktą simbolį, nutraukimą ir meniu ribas mažuose ekranuose.

### 4. Mikrofonas ir leidimas

- Mikrofono mygtukas yra viršuje dešinėje greta LT/EN žymos. Paspaudus paleidžiamas prieinamas Android balso įvedimas; naršyklėje naudojamos tik jos tikrai palaikomos galimybės.
- Leidimų indikatorius tampa žalias tik kai leidimas iš tikrųjų suteiktas. Atmetus leidimą arba nesant atpažinimo paslaugos vartotojui parodoma tikroji būsena.
- Patikrinti suteiktą, atmestą ir nepalaikomą būsenas; rezultato neimituoti kaip tikro mikrofono veikimo.

### 5. Emoji

- Pateikti emoji parinkiklį su kategorijomis ir galimybe įvesti pasirinktas emoji. Išlaikyti anksčiau numatytą istoriją, kai ji jau palaikoma.
- OS šriftas gali nepalaikyti dalies naujų simbolių: nevaizduoti visų simbolių suderinamumo kaip patikrinto vien pagal katalogo dydį.
- Patikrinti kategorijos pasirinkimą, įterpimą ir atvaizdavimą tikrame įrenginyje.

### 6. LT/EN ir PC/QWERTY išdėstymai

- LT ir EN kalbos atskirai leidžia rinktis PC arba QWERTY išdėstymą; atitinkami klavišai ir Shift simboliai turi atitikti pasirinktą variantą.
- Pasirinkimai ir naudotojo išdėstymo pakeitimai išlieka keičiant variantus ir iš naujo paleidus programą.
- Patikrinti visus keturis derinius, realiai įvestus simbolius ir nustatymų išsaugojimą.

### 7. Fn ir F1–F8

- Aktyvinus Fn visi aštuoni klavišai F1–F8 rodomi toje pačioje eilėje; F8 neturi dingti už ekrano ar dėl šrifto dydžio.
- Paspaudus siunčiamas atitinkamas veiksmas ten, kur priimanti programa jį palaiko. Simuliatoriuje aiškiai atskiriama demonstracija nuo OS lygio funkcijų.
- Patikrinti kiekvieną klavišą, ypač F8, siaurą ekraną ir Fn išjungimą.

### 8. Combo kombinacijos

- Kombinaciją sukurti spaudžiant pačius klavišus, pavyzdžiui, Ctrl+C. Įrašant kombinaciją jos veiksmas neturi būti netyčia įvykdytas.
- Išsaugotas kombinacijas galima įjungti, išjungti ir ištrinti; veiksmų juostoje rodomos tik įjungtos. Veiksmas turi būti perduodamas programai pagal platformos galimybes.
- Patikrinti sukūrimą, išsaugojimą, išjungimą, pakartotinį įjungimą, trynimą ir vykdymą palaikančioje programoje.

### 9. Gyva klaviatūros peržiūra

- Peržiūra rodo pasirinktą kalbą, išdėstymo variantą ir klavišus; įjungus Fn arba Combo iškart atvaizduojamos atitinkamos juostos.
- Peržiūros būsenos turi atspindėti tai, kas realiai pasirinkta, o ne statinį demonstracinį paveikslą.
- Patikrinti pasirinkimų keitimą ir atitiktį tikrai Android klaviatūrai.

### 10. Laikomos rodyklės

- Laikant rodyklę žymeklis pradeda kartotinai judėti pasirinkta kryptimi; atleidus, nutraukus veiksmą arba klaviatūrai dingus kartojimas sustoja.
- Jeigu kartu naudojami palaikomi modifikatoriai, jų būsena išlaikoma.
- Patikrinti visas kryptis, laikymą, atleidimą, piršto nuslydimą ir klaviatūros uždarymą.

### 11. Paspaudimo animacija

- Klavišo paspaudimas pateikia vizualų grįžtamąjį ryšį. Nustatymuose esantis Button Press Animation jungiklis įjungia arba išjungia šį efektą.
- Pasirinkimas išlieka tarp paleidimų; išjungta animacija netrukdo pačiam klavišo veiksmui.
- Patikrinti raidžių, papildomų klavišų, Fn ir Combo reakciją bei jungiklio išsaugojimą.

### 12. Clipboard ir Snippets

- Iškarpinėje pateikti platformos leidžiamą turinį ir jau numatytus istorijos veiksmus. Slaptažodžio laukams neturi būti renkama iškarpinės istorija.
- Snippets yra išsaugomi teksto fragmentai. Kuriant neturi būti atskiro pavadinimo laukelio; įterpiamas visas pasirinkto fragmento tekstas, įskaitant naujas eilutes.
- Patikrinti išsaugojimą, įterpimą, trynimą ir apsaugą slaptažodžio laukelyje; naršyklės apribojimus pateikti aiškiai.

### 13. Išdėstymo redagavimas

- Redagavimo režime galima pakeisti arba sukeisti klavišus. Ilgai paspaudus suteikiamas Delete veiksmas.
- Reset to defaults po patvirtinimo atkuria numatytą išdėstymą; atšaukus naudotojo pakeitimai turi likti.
- Patikrinti ištrynimą, patvirtintą ir atšauktą atkūrimą bei išsaugojimą.

### 14. Nustatymai ir profiliai

- Installed Keyboards valdiklis atskleidžia prieinamą pasirinkimą pagal platformos galimybes. Import ir Export veiksmai išdėstyti vienoje eilėje.
- Profilio tekstas turi būti įskaitomas ir kontrastingas; išsaugoti nustatymai ir profilių duomenys turi išlikti.
- Patikrinti sąrašo atidarymą, importo ir eksporto eigą, teksto įskaitomumą bei siaurą ekraną.

## Bendroji patikra

1. Sulyginti kiekvieną punktą su realiu Android kodu ir simuliatoriumi; atlikti trūkstamus pakeitimus, išlaikant jau veikiančius.
2. Paleisti tikslingus automatinius testus ir Android kompiliavimo patikras. Simuliatorių išbandyti naršyklėje, įskaitant siaurą ekraną.
3. Aiškiai atskirti automatinių testų ir tikro telefono rezultatus. Nežymėti rankinio bandymo atliktu, jeigu įrenginiu nebandyta.
4. iOS šioje darbų dalyje neliečiama. Nekelti iOS pakeitimų ir nevadinti nepatikrintų Android ar simuliatoriaus funkcijų užbaigtomis.

## Patikslintos konfigūracijos skydelių detalės

Šios detalės atkurtos iš ankstesnių vartotojo nurodymų ir papildo aukščiau esančius punktus.

- **Profiles:** Import ir Export yra toje pačioje eilėje. „Clone Active Layout as New Profile“ turi reikalauti naujo pavadinimo ir nukopijuoti aktyvų išdėstymą. „New Profile Name“ ir „App package“ įvesties tekstas baltas tamsiame fone, užuominos šviesiai pilkos; „Automatically choose profile per app“ tekstas baltas, varnelė šviesiai mėlyna. Profilių pasirinkimas pagal programą turi išlikti įjungus, išjungus ir importavus nustatymus.
- **Installed Keyboards:** išskleidžiamajame sąraše parodyti įdiegtas ir įjungtas klaviatūras; išjungtos klaviatūros įjungimą nukreipti į Android sistemos nustatymus. Simuliatorius neturi teigti matantis sistemos sąrašą, jei naršyklė to negali.
- **Permissions:** nustatymų skydelyje mažas „Permission“ mygtukas atidaro leidimų suteikimo ar nustatymų veiksmą. Šalia žaliai rodoma „Permissions Granted“ tik kai reikalingas leidimas tikrai suteiktas.
- **Appearance:** viena „Button Press Animation“ varnelė įjungia arba išjungia paspaudimo animaciją. Temos, šrifto, klavišų aukščio ir tarpų nustatymai turi išlikti; peržiūra atsinaujina pakeitus nustatymus.
- **Live Keyboard Preview:** automatiškai rodomas visas dabartinis išdėstymas, aktyvus Fn ir įjungtos Combo kombinacijos, ne tik viena jų dalis.
- **Combo:** yra mygtuko pavadinimo ir veiksmo laukai; Action užregistruoja faktiškai spaudžiamus klavišus ir nuosekliai rodo, pavyzdžiui, „Ctrl“, po to „Ctrl + C“. Nereikalingos pasirinkimų eilutės nėra. Esamas naudojamas kombinacijas turi būti galima įjungti, išjungti ir ištrinti.
- **Clipboard:** istorijos sąraše kopijuotus tekstus galima vienu paspaudimu įterpti, prisegti, ištrinti arba išvalyti; išsaugojimas priklauso nuo platformos ir vartotojo pasirinkimo.
- **Fn:** F1–F8 pateikiami vienoje eilėje be horizontalaus slinkimo; visi mygtukai pakankamai siauri, kad F8 būtų matomas ir mažame ekrane.

- **Saugi bandomoji Android versija:** viešos 0.3 ir vietinės versijos parašai skirtingi. 3.1 peržiūros APK turi atskirą programos identifikatorių ir aiškų „DevBoard Test“ pavadinimą, kad galėtų būti įdiegta greta senosios versijos neištrinant jos duomenų.
- **Profilių duomenų sauga:** Android ir simuliatoriaus importas skaito tarpusavyje suderintus profilių, aktyvaus profilio, programų susiejimų ir automatinio pasirinkimo laukus. Esant jau turimų profilių ar susiejimų konfliktui prašoma patvirtinimo; atšaukus jokie duomenys nekeičiami.
