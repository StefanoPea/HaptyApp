# HaptyApp - Digital Twin Companion
HaptyApp è l'applicazione Android progettata come controparte digitale (Digital Twin) per l'ecosistema [HaptyHub](https://github.com/DavideFantasia/HaptyHub). L'applicazione permette di visualizzare e interagire con i diagrammi e i grafi generati tramite l'applicazione desktop, fungendo da strato interattivo visivo da sovrapporre o affiancare ai modelli tattili stampati in 3D.

L'app è sviluppata in `Kotlin` e utilizza i dati spaziali prodotti dal motore di layout ELK per garantire una corrispondenza millimetrica tra gli elementi visivi su schermo e i rilievi fisici del modello tattile.

## Funzionalità Principali

- **Visualizzazione Digital Twin**: Rendering dinamico di flowchart e grafi basato sui file di coordinate JSON generati dall'applicazione desktop.
- **Interattività Tattile**: Supporto per la navigazione e l'interazione con i nodi e gli archi del diagramma.
- **Ottimizzazione per Tablet**: Layout studiato per massimizzare l'area di lavoro sui dispositivi tablet, facilitando la sovrapposizione con gli overlay stampati.
- **Sincronizzazione Coordinate**: Utilizzo rigoroso delle unità di misura e dei margini definiti nel profilo dispositivo per assicurare la coerenza spaziale.

## Requisiti e Build
Per compilare ed eseguire il progetto è necessario disporre di:
- Android Studio Jellyfish o superiore.
- JDK 17.
- Android SDK 34 (API Level 34).
- Un dispositivo Android (preferibilmente tablet) con supporto per il tocco multipunto.

## Installazione
1. Clonare il repository:

```Bash
git clone git@github.com:stefanopea/haptyapp.git
```
2. Aprire il progetto con Android Studio.
3. Sincronizzare i file di Gradle (KTS).
4. Eseguire l'applicazione su un dispositivo fisico o emulatore.

## Guida all'uso
L'applicazione carica automaticamente le configurazioni grafiche per visualizzare il modello haptico corrispondente.

1. **Caricamento Dati**: L'app legge il file output_coordinates.json situato nella cartella degli asset. Questo file contiene le posizioni (x, y) e le dimensioni di ogni nodo calcolate dall'IA e dal motore ELK.
2. **Visualizzazione**: All'avvio, l'interfaccia principale disegna i nodi (cerchi, quadrati, rombi) e le connessioni seguendo le coordinate del file.
3. **Interazione**: L'utente può toccare gli elementi su schermo per ricevere feedback o per sincronizzare la posizione della mano durante l'esplorazione del modello fisico.

## Struttura del Progetto
```Plaintext
HaptyApp/
├── app/
│   ├── build.gradle.kts        # Configurazione build e dipendenze Android
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml  # Dichiarazione componenti e permessi
│       │   ├── assets/              # Contiene i file JSON di test e configurazione
│       │   ├── java/com/example/digitaltwinflowchart/
│       │   │   └── MainActivity.kt  # Logica principale di rendering e gestione eventi
│       │   └── res/
│       │       ├── layout/          # Definizioni dell'interfaccia utente (XML)
│       │       └── values/          # Risorse di sistema (stringhe, colori, temi)
├── build.gradle.kts            # Configurazione Gradle a livello di progetto
├── settings.gradle.kts         # Definizione dei moduli del progetto
└── README.md                   # Documentazione del repository
```
# Note Tecniche
- **Rendering**: La visualizzazione è gestita tramite componenti custom che interpretano le gerarchie dei grafi ELK.
- **Coordinate**: Le posizioni sono espresse in unità relative che vengono scalate in tempo reale in base alla densità di pixel (DPI) e alle dimensioni fisiche dello schermo del tablet impostato nel sistema _HaptyHub_.
