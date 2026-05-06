// Main app + state machine
const { useState, useEffect, useRef, useMemo } = React;

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "primaryHue": 215,
  "density": "comfortable",
  "radius": 16,
  "showStepper": true,
  "language": "pt"
}/*EDITMODE-END*/;

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

  const [time, setTime] = useState('');
  useEffect(() => {
    const update = () => {
      const d = new Date();
      setTime(`${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`);
    };
    update();
    const id = setInterval(update, 30_000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    const root = document.documentElement;
    const h = tweaks.primaryHue;
    root.style.setProperty('--primary', `oklch(0.50 0.16 ${h})`);
    root.style.setProperty('--primary-dark', `oklch(0.38 0.18 ${h})`);
    root.style.setProperty('--primary-light', `oklch(0.66 0.14 ${h})`);
    root.style.setProperty('--primary-50', `oklch(0.96 0.03 ${h})`);
    root.style.setProperty('--primary-100', `oklch(0.90 0.06 ${h})`);
    root.style.setProperty('--radius', tweaks.radius + 'px');
    root.style.setProperty('--radius-lg', (tweaks.radius * 1.5) + 'px');
  }, [tweaks]);

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
    const code = 'PE' + Math.floor(Math.random() * 9000 + 1000) + '-' + Math.floor(Math.random()*9000+1000);
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

  const stepperOn = tweaks.showStepper !== false;

  return (
    <div className="stage" data-screen-label="Kiosk">
      {screen !== 'idle' && (
        <>
          <StatusBar lang={lang} setLang={setLang} time={time}/>
          {stepperOn && <Stepper step={step} t={t}/>}
        </>
      )}

      {screen === 'idle' && <IdleScreen onStart={start} t={t}/>}

      {screen === 'flow' && step === 0 && (
        <CityScreen value={city} onPick={setCity} t={t} lang={lang}/>
      )}
      {screen === 'flow' && step === 1 && (
        <DateScreen value={date} onPick={setDate} t={t} lang={lang}/>
      )}
      {screen === 'flow' && step === 2 && (
        <TripScreen city={city} date={date} value={trip} onPick={setTrip} onChangeDate={setDate} t={t} lang={lang}/>
      )}
      {screen === 'flow' && step === 3 && (
        <RoomScreen city={city} value={room} onPick={setRoom} t={t} lang={lang}/>
      )}
      {screen === 'flow' && step === 4 && (
        <PassengerScreen value={passenger} onChange={setPassenger} t={t} lang={lang}/>
      )}
      {screen === 'flow' && step === 5 && (
        <PaymentScreen method={payMethod} setMethod={setPayMethod} onApprove={handleApprove} t={t} lang={lang} summary={summary}/>
      )}
      {screen === 'confirm' && booking && (
        <ConfirmScreen booking={booking} onReset={reset} onPrint={() => {}} t={t} lang={lang}/>
      )}

      {screen === 'flow' && step !== 5 && (
        <Footer
          onBack={back}
          onContinue={next}
          continueLabel={t.continue}
          continueDisabled={!canContinue}
          t={t}
          extra={total > 0 ? (
            <div style={{textAlign:'right'}}>
              <div style={{fontSize:11, fontWeight:700, color:'var(--text-muted)', textTransform:'uppercase', letterSpacing:'0.08em'}}>{t.totalCaps}</div>
              <div style={{fontSize:22, fontWeight:800, color:'var(--primary-dark)', letterSpacing:'-0.02em', fontVariantNumeric:'tabular-nums'}}>R$ {total.toFixed(2).replace('.',',')}</div>
            </div>
          ) : null}
        />
      )}
      {screen === 'flow' && step === 5 && (
        <Footer onBack={back} t={t}/>
      )}

      <KioskTweaks tweaks={tweaks} setTweak={setTweak} setLang={setLang}/>
    </div>
  );
}

function KioskTweaks({ tweaks, setTweak, setLang }) {
  return (
    <TweaksPanel title="Tweaks">
      <TweakSection title="Brand">
        <TweakSlider label="Primary hue" value={tweaks.primaryHue} min={180} max={260} step={5} onChange={(v) => setTweak('primaryHue', v)}/>
        <TweakSlider label="Corner radius" value={tweaks.radius} min={6} max={28} step={2} onChange={(v) => setTweak('radius', v)}/>
      </TweakSection>
      <TweakSection title="Layout">
        <TweakToggle label="Show stepper" value={tweaks.showStepper} onChange={(v) => setTweak('showStepper', v)}/>
      </TweakSection>
      <TweakSection title="Language">
        <TweakRadio label="Default" value={tweaks.language}
          options={[{label:'Português', value:'pt'},{label:'English', value:'en'}]}
          onChange={(v) => { setTweak('language', v); setLang(v); }}/>
      </TweakSection>
    </TweaksPanel>
  );
}

function MountedStage() {
  const ref = useRef(null);
  useEffect(() => {
    const el = ref.current;
    const fit = () => {
      const sx = window.innerWidth / 1280;
      const sy = window.innerHeight / 800;
      el.style.transform = `scale(${Math.min(sx, sy)})`;
    };
    fit();
    window.addEventListener('resize', fit);
    return () => window.removeEventListener('resize', fit);
  }, []);
  return (
    <div className="stage-host">
      <div ref={ref}>
        <App/>
      </div>
    </div>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<MountedStage/>);
