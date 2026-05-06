// Shared icon set + small UI primitives
const Icon = ({ name, size = 22, stroke = 2, className = '' }) => {
  const props = {
    width: size, height: size, viewBox: '0 0 24 24', fill: 'none',
    stroke: 'currentColor', strokeWidth: stroke, strokeLinecap: 'round', strokeLinejoin: 'round',
    className,
  };
  switch (name) {
    case 'ferry': return (<svg {...props}><path d="M2 20a3 3 0 0 0 2-1 3 3 0 0 0 4 0 3 3 0 0 0 4 0 3 3 0 0 0 4 0 3 3 0 0 0 4 0 3 3 0 0 0 2 1"/><path d="M4 18l-2-6h20l-2 6"/><path d="M5 12V7h14v5"/><path d="M12 7V3"/></svg>);
    case 'arrow-right': return (<svg {...props}><path d="M5 12h14M13 5l7 7-7 7"/></svg>);
    case 'arrow-left': return (<svg {...props}><path d="M19 12H5M11 19l-7-7 7-7"/></svg>);
    case 'check': return (<svg {...props}><path d="M5 12l5 5L20 7"/></svg>);
    case 'check-big': return (<svg {...props} strokeWidth="3"><path d="M5 12l5 5L20 7"/></svg>);
    case 'plus': return (<svg {...props}><path d="M12 5v14M5 12h14"/></svg>);
    case 'minus': return (<svg {...props}><path d="M5 12h14"/></svg>);
    case 'x': return (<svg {...props}><path d="M18 6L6 18M6 6l12 12"/></svg>);
    case 'clock': return (<svg {...props}><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>);
    case 'pin': return (<svg {...props}><path d="M12 22s7-7 7-12a7 7 0 0 0-14 0c0 5 7 12 7 12z"/><circle cx="12" cy="10" r="2.5"/></svg>);
    case 'qr': return (<svg {...props}><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><path d="M14 14h3v3M21 14v3M14 21h7M17 17v4"/></svg>);
    case 'card': return (<svg {...props}><rect x="2" y="6" width="20" height="13" rx="2"/><path d="M2 11h20M6 15h4"/></svg>);
    case 'user': return (<svg {...props}><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></svg>);
    case 'help': return (<svg {...props}><circle cx="12" cy="12" r="9"/><path d="M9.5 9a2.5 2.5 0 1 1 4 2c-1 .7-1.5 1.4-1.5 2.5"/><circle cx="12" cy="17" r=".7" fill="currentColor"/></svg>);
    case 'sound': return (<svg {...props}><path d="M11 5L6 9H3v6h3l5 4V5z"/><path d="M16 9a4 4 0 0 1 0 6"/></svg>);
    case 'wheelchair': return (<svg {...props}><circle cx="12" cy="4" r="2"/><path d="M10 6v6h6l3 6M10 12a5 5 0 1 0 5 5"/></svg>);
    case 'sparkle': return (<svg {...props}><path d="M12 3l1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5L12 3z"/></svg>);
    case 'printer': return (<svg {...props}><rect x="6" y="3" width="12" height="6" rx="1"/><path d="M6 14H4a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="7" rx="1"/></svg>);
    default: return null;
  }
};

const Stepper = ({ step, t }) => {
  const labels = [
    { pt: 'Cidade', en: 'City' },
    { pt: 'Data', en: 'Date' },
    { pt: 'Viagem', en: 'Trip' },
    { pt: 'Cômodo', en: 'Room' },
    { pt: 'Dados', en: 'Details' },
    { pt: 'Pagto.', en: 'Pay' },
  ];
  const lang = t === I18N.pt ? 'pt' : 'en';
  return (
    <div className="stepper">
      {labels.map((l, i) => (
        <React.Fragment key={i}>
          <div className={`step ${i < step ? 'done' : ''} ${i === step ? 'active' : ''}`}>
            <div className="step-dot">
              {i < step ? <Icon name="check" size={14} stroke={3}/> : i + 1}
            </div>
            <div className="step-label">{l[lang]}</div>
          </div>
          {i < labels.length - 1 && (
            <div className={`step-line ${i < step ? 'done step' : ''}`} style={i < step ? {background:'transparent'} : {}}>
              <div style={{
                position:'absolute', inset:0, background:'var(--primary)',
                transform: i < step ? 'scaleX(1)' : 'scaleX(0)',
                transformOrigin:'left',
                transition:'transform 400ms var(--ease-out)'
              }}/>
            </div>
          )}
        </React.Fragment>
      ))}
    </div>
  );
};

const StatusBar = ({ lang, setLang, time }) => (
  <div className="status-bar">
    <div className="brand">
      <div className="brand-mark"><Icon name="ferry" size={20} stroke={2.2}/></div>
      <div>Passagens Express</div>
    </div>
    <div className="meta">
      <div className="row">
        <Icon name="pin" size={14}/>
        <span>Term. Marítimo</span>
      </div>
      <div>{time}</div>
      <div className="lang-toggle">
        <button className={lang === 'pt' ? 'active' : ''} onClick={() => setLang('pt')}>PT</button>
        <button className={lang === 'en' ? 'active' : ''} onClick={() => setLang('en')}>EN</button>
      </div>
    </div>
  </div>
);

const Footer = ({ onBack, onContinue, continueLabel, continueDisabled, t, helpRight = true, extra }) => (
  <div className="screen-footer">
    {onBack ? (
      <button className="btn btn-ghost" onClick={onBack}>
        <Icon name="arrow-left" size={20}/>
        {t.back}
      </button>
    ) : <div/>}
    <div style={{display:'flex', alignItems:'center', gap:16}}>
      {extra}
      {helpRight && <div className="muted" style={{fontSize:13, display:'flex', alignItems:'center', gap:6}}>
        <Icon name="help" size={16}/>{t.needHelp}
      </div>}
      {onContinue && (
        <button className="btn btn-primary" onClick={onContinue} disabled={continueDisabled}>
          {continueLabel || t.continue}
          <Icon name="arrow-right" size={20}/>
        </button>
      )}
    </div>
  </div>
);

Object.assign(window, { Icon, Stepper, StatusBar, Footer });
