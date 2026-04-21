
import os
import pandas as pd
import streamlit as st
import plotly.graph_objects as go
import plotly.express as px

# --------------------
# Page
# --------------------
st.set_page_config(
    page_title="EEG – Prédictions ML",
    page_icon="🧠",
    layout="wide",
)

# --------------------
# Chemins
# --------------------
ROOT         = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
PARQUET_PATH = os.path.join(ROOT, "output", "datasetpredection.parquet")

DISORDER_COLORS = {
    "Addictive disorder":                  "#E57373",
    "Trauma and stress related disorder":  "#FFB74D",
    "Mood disorder":                       "#4FC3F7",
    "Healthy control":                     "#81C784",
    "Obsessive compulsive disorder":       "#CE93D8",
    "Schizophrenia":                       "#F06292",
    "Anxiety disorder":                    "#FFF176",
}

# --------------------
# Chargement
# --------------------
@st.cache_data
def load_data() -> pd.DataFrame:
    df = pd.read_parquet(PARQUET_PATH)
    df = df.sample(50, random_state=42)
    # Dénormalisation approximative pour affichage lisible
    df["age_ans"]   = (df["age"]       * (71.88 - 18) + 18).round(0).astype(int)
    df["edu_ans"]   = (df["education"] * 20).round(0).astype(int)
    df["iq_val"]    = (df["iq"]        * (145 - 49) + 49).round(0).astype(int)
    df["sexe"]      = df["sex"].map({0: "Homme", 1: "Femme"})
    df["statut"]    = df["is_patient_pred"].map({0: "Sain", 1: "Malade"})
    df["correct"]   = df["is_patient_true"] == df["is_patient_pred"]
    return df

# --------------------
# Carte de prédiction
# --------------------
def render_card(p: pd.Series):
    is_sick      = int(p["is_patient_pred"]) == 1
    disorder     = p["predicted_disorder"]
    bin_conf     = float(p["binary_confidence"])
    multi_conf   = float(p["multi_confidence"])
    correct      = bool(p["correct"])

    border  = "#E57373" if is_sick else "#81C784"
    emoji   = "🔴" if is_sick else "🟢"
    d_color = DISORDER_COLORS.get(disorder, "#90A4AE")
    check   = "✅" if correct else "❌"

    st.markdown(f"""
    <div style="border:2px solid {border}; border-radius:14px;
                padding:20px; background:#1E1E2E;">
        <h3 style="margin:0 0 14px 0; color:#E0E0E0;">
            {emoji} Patient #{int(p['id'])}
            <span style="float:right; font-size:0.75em; color:#9E9E9E;">
                Prédiction correcte : {check}
            </span>
        </h3>
        <div style="display:flex; gap:36px; flex-wrap:wrap; margin-bottom:14px;">
            <div>
                <div style="color:#9E9E9E; font-size:0.75em;">ÂGE</div>
                <div style="font-size:1.2em; color:#E0E0E0;"><b>{p['age_ans']} ans</b></div>
            </div>
            <div>
                <div style="color:#9E9E9E; font-size:0.75em;">SEXE</div>
                <div style="font-size:1.2em; color:#E0E0E0;"><b>{p['sexe']}</b></div>
            </div>
            <div>
                <div style="color:#9E9E9E; font-size:0.75em;">ÉDUCATION</div>
                <div style="font-size:1.2em; color:#E0E0E0;"><b>{p['edu_ans']} ans</b></div>
            </div>
            <div>
                <div style="color:#9E9E9E; font-size:0.75em;">QI</div>
                <div style="font-size:1.2em; color:#E0E0E0;"><b>{p['iq_val']}</b></div>
            </div>
            <div>
                <div style="color:#9E9E9E; font-size:0.75em;">TROUBLE RÉEL</div>
                <div style="font-size:1.1em; color:#FFCC80;"><b>{p['main_disorder']}</b></div>
            </div>
        </div>
        <hr style="border-color:#333; margin:10px 0;">
        <div style="display:flex; gap:36px; flex-wrap:wrap;">
            <div>
                <div style="color:#9E9E9E; font-size:0.75em;">MODÈLE 1 · SVM</div>
                <div style="font-size:1.3em; color:{border};"><b>{p['statut']}</b></div>
                <div style="color:#B0BEC5; font-size:0.85em;">Confiance : <b>{bin_conf:.1%}</b></div>
            </div>
            <div>
                <div style="color:#9E9E9E; font-size:0.75em;">MODÈLE 2 · XGBoost</div>
                <div style="font-size:1.2em; color:{d_color};"><b>{disorder}</b></div>
                <div style="color:#B0BEC5; font-size:0.85em;">Confiance : <b>{multi_conf:.1%}</b></div>
            </div>
        </div>
    </div>
    """, unsafe_allow_html=True)

# --------------------
# APP
# --------------------
st.markdown("""
<h1 style='margin-bottom:0;'>🧠 EEG – Prédictions Psychiatriques</h1>
""", unsafe_allow_html=True)
st.divider()

# ── Chargement 
try:
    df = load_data()
except FileNotFoundError:
    st.error(f"Fichier introuvable : `{PARQUET_PATH}`\n\nLancez MS1 puis le notebook ML.")
    st.stop()

# ── Sidebar : filtres + stats 
with st.sidebar:
    st.header("Filtres")

    sexe_options = ["Tous", "Homme", "Femme"]
    filtre_sexe  = st.selectbox("Sexe", sexe_options)

    all_disorders = sorted(df["main_disorder"].unique().tolist())
    filtre_trouble = st.multiselect(
        "Trouble réel",
        options=all_disorders,
        default=all_disorders,
    )

    filtre_statut = st.radio(
        "Statut prédit",
        options=["Tous", "Malade", "Sain"],
        horizontal=True,
    )

    filtre_correct = st.checkbox("Uniquement les prédictions correctes", value=False)

# ── Application des filtres ──────────────────────────────
filtered = df.copy()

if filtre_sexe != "Tous":
    filtered = filtered[filtered["sexe"] == filtre_sexe]

if filtre_trouble:
    filtered = filtered[filtered["main_disorder"].isin(filtre_trouble)]

if filtre_statut == "Malade":
    filtered = filtered[filtered["is_patient_pred"] == 1]
elif filtre_statut == "Sain":
    filtered = filtered[filtered["is_patient_pred"] == 0]

if filtre_correct:
    filtered = filtered[filtered["correct"]]

# ── En-tête résultats ────────────────────────────────────
st.markdown(f"**{len(filtered):,} patient(s) correspondent aux filtres**")

if filtered.empty:
    st.warning("Aucun patient ne correspond aux filtres sélectionnés.")
    st.stop()

# ── Sélection du patient ─────────────────────────────────
col_sel, col_nav = st.columns([3, 1])

patient_ids = filtered["id"].tolist()

with col_sel:
    selected_id = st.selectbox(
        "Choisir un patient (ID)",
        options=patient_ids,
        format_func=lambda pid: (
            f"Patient {pid}  –  "
            f"{filtered.loc[filtered['id']==pid, 'age_ans'].values[0]} ans  ·  "
            f"{filtered.loc[filtered['id']==pid, 'sexe'].values[0]}  ·  "
            f"{filtered.loc[filtered['id']==pid, 'main_disorder'].values[0]}"
        ),
    )

with col_nav:
    st.markdown("<br>", unsafe_allow_html=True)
    col_prev, col_next = st.columns(2)
    idx_current = patient_ids.index(selected_id)

st.divider()

# ── Affichage du patient sélectionné ────────────────────
patient = filtered[filtered["id"] == selected_id].iloc[0]

col_card, col_gauges = st.columns([1, 1])

with col_card:
    render_card(patient)

with col_gauges:
    bin_conf   = float(patient["binary_confidence"])
    multi_conf = float(patient["multi_confidence"])
    is_sick    = int(patient["is_patient_pred"]) == 1
    disorder   = patient["predicted_disorder"]

    fig_g1 = go.Figure(go.Indicator(
        mode  = "gauge+number",
        value = bin_conf * 100,
        number= {"suffix": "%", "font": {"size": 30}},
        title = {"text": "Confiance SVM<br><sub>Malade / Sain</sub>"},
        gauge = {
            "axis" : {"range": [0, 100]},
            "bar"  : {"color": "#E57373" if is_sick else "#81C784"},
            "steps": [
                {"range": [0,  50], "color": "#1E1E2E"},
                {"range": [50, 75], "color": "#263238"},
                {"range": [75, 100],"color": "#2E3B4E"},
            ],
            "threshold": {"line": {"color": "white", "width": 2}, "value": 75},
        },
    ))
    fig_g1.update_layout(height=200, margin=dict(t=60, b=10, l=20, r=20))
    st.plotly_chart(fig_g1, use_container_width=True)

    fig_g2 = go.Figure(go.Indicator(
        mode  = "gauge+number",
        value = multi_conf * 100,
        number= {"suffix": "%", "font": {"size": 30}},
        title = {"text": f"Confiance XGBoost<br><sub>{disorder}</sub>"},
        gauge = {
            "axis" : {"range": [0, 100]},
            "bar"  : {"color": DISORDER_COLORS.get(disorder, "#90A4AE")},
            "steps": [
                {"range": [0,  50], "color": "#1E1E2E"},
                {"range": [50, 75], "color": "#263238"},
                {"range": [75, 100],"color": "#2E3B4E"},
            ],
            "threshold": {"line": {"color": "white", "width": 2}, "value": 75},
        },
    ))
    fig_g2.update_layout(height=200, margin=dict(t=60, b=10, l=20, r=20))
    st.plotly_chart(fig_g2, use_container_width=True)

# ── Tableau des patients filtrés ─────────────────────────
with st.expander(f"Tableau complet ({len(filtered)} patients)"):
    display_cols = {
        "id":                 "ID",
        "age_ans":            "Âge",
        "sexe":               "Sexe",
        "edu_ans":            "Éducation",
        "iq_val":             "QI",
        "main_disorder":      "Trouble réel",
        "statut":             "Prédit",
        "predicted_disorder": "Trouble prédit",
        "binary_confidence":  "Conf. SVM",
        "multi_confidence":   "Conf. XGBoost",
        "correct":            "Correct",
    }
    table = filtered[list(display_cols.keys())].rename(columns=display_cols).reset_index(drop=True)
    table["Conf. SVM"]     = table["Conf. SVM"].map("{:.1%}".format)
    table["Conf. XGBoost"] = table["Conf. XGBoost"].map("{:.1%}".format)
    table["Correct"]       = table["Correct"].map({True: "✅", False: "❌"})
    st.dataframe(table, use_container_width=True, height=300)

# ── Footer ───────────────────────────────────────────────
st.divider()
st.caption("========"
)
