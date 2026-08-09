module.exports = {
  darkMode: "class",
  content: [
    "C:/Users/Caleb/OneDrive/Desktop/Gamified Java by Caleb Mensah/gamified-java-prep/src/main/resources/templates/**/*.html",
    "C:/Users/Caleb/OneDrive/Desktop/Gamified Java by Caleb Mensah/gamified-java-prep/src/main/resources/static/**/*.js"
  ],
  plugins: [
    require("@tailwindcss/forms"),
    require("@tailwindcss/container-queries")
  ],
  theme: {
    extend: {
      colors: {
        "cockpit-base": "#0B0F0E",
        "panel-slate": "#12181A",
        "primary": "#4edea3",
        "on-primary": "#003824",
        "primary-fixed": "#6ffbbe",
        "primary-fixed-dim": "#4edea3",
        "primary-container": "#10b981",
        "ghost-text": "#8A9895",
        "on-surface": "#dde4dd",
        "on-surface-variant": "#bbcabf",
        "primary-text": "#EAF2EF",
        "surface-container": "#1a211d",
        "surface-container-high": "#242c27",
        "surface-container-highest": "#2f3632",
        "surface-container-low": "#161d19",
        "surface-container-lowest": "#09100c",
        "surface-variant": "#2f3632",
        "surface-dim": "#0e1511",
        "surface-bright": "#343b36",
        "surface-tint": "#4edea3",
        "surface": "#0e1511",
        "background": "#0e1511",
        "elevated-graphite": "#1A2224",
        "hairline": "rgba(148,163,161,0.14)",
        "emerald-wash": "rgba(16,185,129,0.12)",
        "secondary": "#f5be3c",
        "secondary-container": "#c29100",
        "fault-red": "#E5533C",
        "error": "#ffb4ab",
        "error-container": "#93000a",
        "on-error": "#690005",
        "outline": "#86948a",
        "outline-variant": "#3c4a42",
        "inverse-surface": "#dde4dd",
        "inverse-on-surface": "#2b322d",
        "inverse-primary": "#006c49",
        "tertiary": "#ffb3af",
        "on-primary-container": "#00422b",
        "on-primary-fixed": "#002113",
        "on-background": "#dde4dd"
      },
      borderRadius: {
        DEFAULT: "0.25rem",
        lg: "0.5rem",
        xl: "0.75rem",
        "2xl": "1rem",
        full: "9999px"
      },
      spacing: {
        gutter: "1.5rem",
        "max-width": "1400px",
        "stage-padding": "2rem"
      },
      fontFamily: {
        "body-md": ["Geist", "sans-serif"],
        "body-lg": ["Geist", "sans-serif"],
        "label-sm": ["JetBrains Mono", "monospace"],
        "headline-lg": ["Geist", "sans-serif"],
        "headline-md": ["Geist", "sans-serif"],
        "code-md": ["JetBrains Mono", "monospace"],
        "stat-lg": ["JetBrains Mono", "monospace"]
      },
      fontSize: {
        "body-md": ["16px", { lineHeight: "1.5", fontWeight: "400" }],
        "label-sm": ["12px", { lineHeight: "1.4", letterSpacing: "0.02em", fontWeight: "500" }],
        "headline-lg": ["32px", { lineHeight: "1.2", letterSpacing: "-0.02em", fontWeight: "700" }],
        "body-lg": ["18px", { lineHeight: "1.6", fontWeight: "400" }],
        "code-md": ["14px", { lineHeight: "1.5", fontWeight: "400" }],
        "stat-lg": ["20px", { lineHeight: "1.2", fontWeight: "600" }],
        "headline-md": ["24px", { lineHeight: "1.3", letterSpacing: "-0.01em", fontWeight: "600" }]
      }
    }
  }
};