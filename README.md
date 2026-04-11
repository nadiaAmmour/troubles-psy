

## Installation des dépendances
```powershell
sbt update
```
Cette commande télécharge automatiquement toutes les bibliothèques nécessaires au projet (lecture CSV, écriture Parquet, logs, tests).

## Output
`output/dataset.parquet` — 945 patients, 1140 features EEG normalisées [0,1]