// Main app + state machine — Minimal redesign
const { useState, useEffect, useRef, useMemo } = React;

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "accent": "#1B4FBE",
  "showBrandSlab": true,
  "language": "pt"
}/*EDITMODE-END*/;

// Section labels shown on the orange brand slab
const SECTION_LABELS = {
  pt: { 0: 'destino', 1: 'quando', 2: 'saída', 3: 'cabine', 4: 'dados', 5: 'pagar', confirm: 'pronto' },
  en: { 0: 'where',   1: 'when',   2: 'depart', 3: 'cabin', 4: 'you',   5: 'pay',  confirm: 'done'  },
};

function App() {
  const [tweaks, setTweak] = useTweaks(TWEAK_DEFAULTS);
  const [lang, setLang] = useState(tweaks.language || 'pt');
  const t = I18N[lang];

  const [screen, setScreen] = useState('idle'); // idle | flow | confirm
  const [step, setStep] = useState(0);
  const [city, setCity] = useState(null);
  const [date, setDate] = useState(null);
  const [trip, setTrip] = useState(null);
  const [room, setRoom] = useState(null);
  const [passenger, setPassenger] = useState({ name: '', cpf: '', phone: '', birth: '' });
  const [payMethod, setPayMethod] = useState('pix');
  const [booking, setBooking] = useState(null);

  useEffect(() => {
    const root = document.documentElement;
    root.style.setProperty('--accent', tweaks.accent);
  }, [tweaks.accent]);

  const cityData = useMemo(() => CITIES.find(c => c.id === city), [city]);
  const roomData = useMemo(() => ROOMS.find(r => r.id === room), [room]);
  const dateData = useMemo(() => DATES.find(d => d.iso === date), [date]);
  const tripData = trip != null ? TRIP_TIMES[trip] : null;

  const total = roomData ? roomData.price : 0;

  const summary = {
    route: cityData ? `Salvador → ${cityData.name}` : '',
    date: dateData ? `${dateData.day} ${lang==='pt'?dateData.mon_pt:dateData.mon_en}` : '',
    dep: tripData ? tripData.dep : '',
    room: roomData ? (lang==='pt'?roomData.name_pt:roomData.name_en) : '',
    total,
  };

  const reset = () => {
    setScreen('idle'); setStep(0);
    setCity(null); setDate(null); setTrip(null); setRoom(null);
    setPassenger({ name: '', cpf: '', phone: '', birth: '' });
    setPayMethod('pix'); setBooking(null);
  };

  const start = () => { setScreen('flow'); setStep(0); };
  const next = () => { if (step < 5) setStep(step + 1); };
  const back = () => { if (step === 0) setScreen('idle'); else setStep(step - 1); };

  const handleApprove = () => {
    const code = 'PE-' + Math.floor(Math.random() * 9000 + 1000) + '-' + Math.floor(Math.random()*9000+1000);
    setBooking({ code, ...summary });
    setScreen('confirm');
  };

  useEffect(() => {
    if (screen !== 'confirm') return;
    const id = setTimeout(reset, 30_000);
    return () => clearTimeout(id);
  }, [screen]);

  const canContinue = (() => {
    if (step === 0) return !!city;
    if (step === 1) return !!date;
    if (step === 2) return trip != null;
    if (step === 3) return !!room;
    if (step === 4) return passenger.name.length > 2 && passenger.cpf.replace(/\D/g,'').length === 11 && passenger.birth.replace(/\D/g,'').length === 8;
    return true;
  })();

  // The section label shown on the orange slab
  const slabKey = screen === 'idle' ? 'idle'
                : screen === 'confirm' ? 'confirm'
                : step;
  const slabLabel = screen === 'idle'
    ? null
    : SECTION_LABELS[lang][slabKey];

  const showBrand = tweaks.showBrandSlab !== false;

  return (
    <div className="compose-host">
      <div className="compose" data-screen-label="Kiosk" style={{
        gridTemplateColumns: showBrand ? '1100px 1fr' : '1fr',
      }}>
        {/* Left: totem hardware containing the touchscreen */}
        <div className="totem-col">
          <div className="totem">
            <div className="speaker"/>
            <div className="stage">
              {screen === 'idle' && <IdleScreen onStart={start} lang={lang} setLang={setLang}/>}
              {screen === 'flow' && (
                <FlowScreen
                  step={step} setStep={setStep}
                  t={t} lang={lang} setLang={setLang}
                  city={city} setCity={setCity}
                  date={date} setDate={setDate}
                  trip={trip} setTrip={setTrip}
                  room={room} setRoom={setRoom}
                  passenger={passenger} setPassenger={setPassenger}
                  payMethod={payMethod} setPayMethod={setPayMethod}
                  next={next} back={back}
                  canContinue={canContinue}
                  total={total}
                  summary={summary}
                  onApprove={handleApprove}
                />
              )}
              {screen === 'confirm' && booking && (
                <ConfirmScreen booking={booking} onReset={reset} t={t} lang={lang}/>
              )}
            </div>
          </div>
        </div>

        {/* Right: orange brand slab with giant typography */}
        {showBrand && (
          <BrandSlab
            screen={screen}
            step={step}
            label={slabLabel}
            lang={lang}
          />
        )}
      </div>

      <KioskTweaks tweaks={tweaks} setTweak={setTweak} setLang={setLang}/>
    </div>
  );
}

function BrandSlab({ screen, step, label, lang }) {
  const num = screen === 'idle' ? '' : screen === 'confirm' ? '✓' : String(step + 1).padStart(2,'0');
  const total = '06';
  // Render label letter-by-letter so we can echo it for that layered look
  return (
    <div className="brand-col" data-screen-label="Brand slab">
      <div className="brand-head">
        <span>Passagens Express</span>
        <span>Term. Marítimo · Salvador</span>
      </div>

      <div style={{position:'relative'}}>
        {label && (
          <h2 className="brand-mega" key={label}>
            {label}
            <span className="echo" aria-hidden="true">{label}</span>
          </h2>
        )}
        {screen === 'idle' && (
          <h2 className="brand-mega" style={{fontSize: 380, writingMode:'horizontal-tb'}}>
            ferry<span className="echo" aria-hidden="true">ferry</span>
          </h2>
        )}
      </div>

      <div className="brand-foot">
        <span className="pe-mark">PE</span>
        {screen === 'flow' && (
          <span className="pe-step">{num} / {total} · {lang === 'pt' ? 'etapa' : 'step'}</span>
        )}
        {screen === 'idle' && (
          <span className="pe-step">{lang === 'pt' ? 'toque a tela' : 'tap to start'}</span>
        )}
        {screen === 'confirm' && (
          <span className="pe-step">{lang === 'pt' ? 'finalizado' : 'completed'}</span>
        )}
      </div>
    </div>
  );
}

/* Flow router — picks the correct screen based on step */
function FlowScreen(props) {
  const { step, t, lang, setLang, next, back, canContinue, total, summary, onApprove } = props;

  const stepLabels = SECTION_LABELS[lang];
  const eyebrow = lang === 'pt'
    ? `Etapa ${step + 1} de 6 · ${stepLabels[step]}`
    : `Step ${step + 1} of 6 · ${stepLabels[step]}`;

  let body = null;
  if (step === 0) body = <CityScreen value={props.city} onPick={props.setCity} t={t} lang={lang}/>;
  if (step === 1) body = <DateScreen value={props.date} onPick={props.setDate} t={t} lang={lang}/>;
  if (step === 2) body = <TripScreen city={props.city} date={props.date} value={props.trip} onPick={props.setTrip} onChangeDate={props.setDate} t={t} lang={lang}/>;
  if (step === 3) body = <RoomScreen city={props.city} value={props.room} onPick={props.setRoom} t={t} lang={lang}/>;
  if (step === 4) body = <PassengerScreen value={props.passenger} onChange={props.setPassenger} t={t} lang={lang}/>;
  if (step === 5) body = <PaymentScreen method={props.payMethod} setMethod={props.setPayMethod} onApprove={onApprove} t={t} lang={lang} summary={summary}/>;

  return (
    <div className="screen">
      <TopBar lang={lang} setLang={setLang} eyebrow={eyebrow}/>
      <div className="screen-body screen-enter" key={step}>
        {body}
      </div>
      <Footer
        onBack={back}
        onContinue={step === 5 ? null : next}
        continueDisabled={!canContinue}
        t={t}
        total={total}
        showTotal={step >= 3 && step < 5}
      />
    </div>
  );
}

function TopBar({ lang, setLang, eyebrow }) {
  return (
    <div className="top-bar">
      <div className="step-counter">{eyebrow}</div>
      <div className="pill-group">
        <button
          className="pill"
          onClick={() => setLang(lang === 'pt' ? 'en' : 'pt')}
          aria-label="Toggle language"
        >
          <Icon name="globe" size={18}/>
          <span>{lang === 'pt' ? 'PT' : 'EN'}</span>
        </button>
        <button className="icon-btn" aria-label="Help"><Icon name="help" size={20}/></button>
      </div>
    </div>
  );
}

function Footer({ onBack, onContinue, continueDisabled, t, total, showTotal }) {
  return (
    <div className="screen-foot">
      <button className="back-link" onClick={onBack}>
        <Icon name="arrow-left" size={18}/>
        {t.back}
      </button>
      <div className="foot-extra">
        {showTotal && total > 0 && (
          <div className="total-tag">
            <div className="lbl">{t.totalCaps}</div>
            <div className="val">R$ {total.toFixed(2).replace('.',',')}</div>
          </div>
        )}
        {onContinue && (
          <button className="cta accent" onClick={onContinue} disabled={continueDisabled}>
            {t.continue}
            <Icon name="arrow-right" size={18}/>
          </button>
        )}
      </div>
    </div>
  );
}

function KioskTweaks({ tweaks, setTweak, setLang }) {
  return (
    <TweaksPanel title="Tweaks">
      <TweakSection title="Brand">
        <TweakColor
          label="Accent"
          value={tweaks.accent}
          onChange={(v) => setTweak('accent', v)}
          options={['#1B4FBE', '#0D3D7A', '#0E5C7A', '#1A1814', '#E85D1F']}
        />
        <TweakToggle
          label="Show brand slab"
          value={tweaks.showBrandSlab}
          onChange={(v) => setTweak('showBrandSlab', v)}
        />
      </TweakSection>
      <TweakSection title="Language">
        <TweakRadio
          label="Default"
          value={tweaks.language}
          options={[{label:'Português', value:'pt'},{label:'English', value:'en'}]}
          onChange={(v) => { setTweak('language', v); setLang(v); }}
        />
      </TweakSection>
    </TweaksPanel>
  );
}

function MountedStage() {
  const ref = useRef(null);
  useEffect(() => {
    const el = ref.current;
    const fit = () => {
      const sx = window.innerWidth / 2400;
      const sy = window.innerHeight / 1500;
      el.style.transform = `scale(${Math.min(sx, sy)})`;
    };
    fit();
    window.addEventListener('resize', fit);
    return () => window.removeEventListener('resize', fit);
  }, []);
  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      background: '#1A1814',
      overflow: 'hidden',
    }}>
      <div
        ref={ref}
        style={{
          position: 'absolute',
          width: 2400,
          height: 1500,
          top: '50%',
          left: '50%',
          marginTop: -750,
          marginLeft: -1200,
          transformOrigin: 'center center',
        }}
      >
        <App/>
      </div>
    </div>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<MountedStage/>);
