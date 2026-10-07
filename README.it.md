# HaptyApp

*Altre lingue: [English](README.md)*

HaptyApp è l'applicazione Android che accompagna i diagrammi di flusso tattili stampati in 3D.
La tavola stampata si appoggia sul tablet come una mascherina e, quando uno studente cieco tocca un nodo attraverso la sua apertura, l'app legge il nodo ad alta voce.

L'app è stata sviluppata nell'ambito del progetto [HaptyHub](https://github.com/DavideFantasia/HaptyHub).
Le tavole e i file di layout che carica sono prodotti dalla pipeline di [tactile-flowcharts](https://github.com/StefanoPea/tactile-flowcharts), il codice della tesi magistrale *From Static Visual Content to Accessible Interactive Representations: A Tactile Pipeline Using Open Vision-Language Models* (Università di Pisa).

## Come funziona

- **File di layout.** L'app carica un file di layout (`app_layout.json`, scritto da `pipeline/pipeline.py` in tactile-flowcharts) con i nodi, gli archi e il centro di ogni apertura della tavola, in millimetri sullo schermo. Sotto ogni apertura colloca una zona sensibile al tocco: poiché aperture e zone provengono dagli stessi dati, coincidono senza bisogno di misure manuali.
- **Voce.** Toccando un nodo, l'app legge in italiano il tipo, il testo e ogni arco che parte dal nodo, con la sua direzione e, per una decisione, l'etichetta Sì o No. Gli operatori nel testo sono letti a parole (`<=` diventa "minore o uguale di"). Sui dispositivi con motore di vibrazione, ogni tocco produce anche una breve vibrazione.
- **Menu.** Un'apertura quadrata senza bordo in rilievo, nell'angolo in alto a destra della tavola, lascia raggiungibile il pulsante del menu.
  - Un tocco cambia quanto di ogni descrizione viene letto (Expertise Menu): *Principiante* legge tipo, testo e archi; *Intermedio* testo e archi; *Esperto* solo il testo.
  - Una pressione prolungata apre la selezione dei file per caricare il file di layout di un'altra tavola, con indicazioni audio. Senza tavola sullo schermo, lo stesso si ottiene con una pressione prolungata sullo sfondo.
- **File precedenti.** I file di layout senza le posizioni delle aperture, come il diagramma di esempio in `assets/`, vengono adattati allo schermo dall'app stessa.

## Requisiti

- Android Studio con Android Gradle Plugin 9.1 e Android SDK 36; Gradle usa JDK 21.
- Un tablet Android con Android 7.0 (API 24) o successivo e una voce di sintesi vocale in italiano.
- Un file di layout generato per le dimensioni dello schermo di quel tablet (`pipeline/devices.json` in tactile-flowcharts).

## Installazione

1. Clonare il repository:

   ```bash
   git clone git@github.com:StefanoPea/HaptyApp.git
   ```

2. Aprire il progetto con Android Studio e sincronizzare i file Gradle.
3. Avviare l'app sul tablet.

## Utilizzo

1. Generare la tavola e il suo file di layout con tactile-flowcharts (`pipeline/pipeline.py`) e stampare la tavola.
2. Copiare `app_layout.json` sul tablet.
3. Aprire l'app e tenere premuto il pulsante del menu per caricare il file.
4. Appoggiare la tavola sul tablet ed esplorarla con il tatto.

## Struttura del progetto

```plaintext
HaptyApp/
├── app/
│   ├── build.gradle.kts             # configurazione della build
│   └── src/main/
│       ├── AndroidManifest.xml      # activity e permessi
│       ├── assets/                  # diagramma di esempio caricato all'avvio
│       ├── java/com/example/digitaltwinflowchart/
│       │   └── MainActivity.kt      # zone sensibili, voce e menu
│       └── res/                     # layout e risorse
├── build.gradle.kts
└── settings.gradle.kts
```

## Note tecniche

- I millimetri sono convertiti in pixel con la densità dello schermo dichiarata da Android (`xdpi`, `ydpi`). Se un dispositivo la dichiara in modo impreciso, le zone sensibili si spostano rispetto alle aperture: su un tablet nuovo conviene verificare l'allineamento prima di usarlo con uno studente.
- La velocità della voce è impostata a 1,5 volte quella predefinita.

## Autori

Davide Fantasia, Stefano Pea e Lorenzo Tinfena.
