// Passenger, Payment, Confirmation
const PassengerScreen = ({ value, onChange, t, lang }) => {
  const [focusKey, setFocusKey] = React.useState('cpf');
  const fields = [
    { key: 'cpf',   label: t.cpf,   full: false, mask: 'cpf' },
    { key: 'birth', label: t.birth, full: false, mask: 'date' },
    { key: 'name',  label: t.name,  full: true },
    { key: 'phone', label: t.phone, full: true,  mask: 'phone' },
  ];
  const formatVal = (k, v) => {
    if (k === 'cpf') return v.replace(/\D/g,'').slice(0,11).replace(/(\d{3})(\d{3})(\d{3})(\d{0,2})/,(_,a,b,c,d)=>d?`${a}.${b}.${c}-${d}`:c?`${a}.${b}.${c}`:b?`${a}.${b}`:a);
    if (k === 'birth') return v.replace(/\D/g,'').slice(0,8).replace(/(\d{2})(\d{2})(\d{0,4})/,(_,a,b,c)=>c?`${a}/${b}/${c}`:b?`${a}/${b}`:a);
    if (k === 'phone') return v.replace(/\D/g,'').slice(0,11).replace(/(\d{2})(\d{5})(\d{0,4})/,(_,a,b,c)=>c?`(${a}) ${b}-${c}`:b?`(${a}) ${b}`:a?`(${a}`:'');
    return v;
  };
  return (
    <div className="screen screen-enter">
      <div className="screen-body">
        <h1 className="screen-title">{t.passengerData}</h1>
        <p className="screen-sub">{t.passengerDataSub}</p>
        <div style={{display:'grid', gridTemplateColumns:'1fr 380px', gap:24, alignItems:'start'}}>
          <div className="form-grid">
            {fields.map(f => (
              <div key={f.key} className={`field ${f.full ? 'full' : ''} ${value[f.key] ? 'filled' : ''} ${focusKey===f.key?'focused':''}`}>
                <label>{f.label}</label>
                <input
                  value={value[f.key] || ''}
                  onFocus={() => setFocusKey(f.key)}
                  onChange={(e) => onChange({ ...value, [f.key]: f.mask ? formatVal(f.key, e.target.value) : e.target.value })}
                  inputMode={f.mask ? 'numeric' : 'text'}
                />
              </div>
            ))}
            <div className="field full" style={{padding:'14px 18px', display:'flex', alignItems:'center', gap:12, background:'var(--primary-50)', borderColor:'var(--primary-100)'}}>
              <Icon name="sparkle" size={18}/>
              <div style={{fontSize:13, color:'var(--primary-dark)', fontWeight:500}}>
                {lang==='pt' ? 'Seus dados são usados apenas para emissão do bilhete.' : 'Your details are used only to issue your ticket.'}
              </div>
            </div>
          </div>
          <Keypad onPress={(k) => {
            const cur = value[focusKey] || '';
            const isMasked = ['cpf','birth','phone'].includes(focusKey);
            let next;
            if (k === 'back') next = cur.slice(0, -1);
            else if (k === 'space') next = cur + ' ';
            else next = cur + k;
            onChange({ ...value, [focusKey]: isMasked ? formatVal(focusKey, next) : next });
          }} t={t} lang={lang} focusKey={focusKey}/>
        </div>
      </div>
    </div>
  );
};

const Keypad = ({ onPress, t, lang, focusKey }) => (
  <div>
    <div style={{fontSize:11, fontWeight:700, letterSpacing:'0.08em', textTransform:'uppercase', color:'var(--text-muted)', marginBottom:10}}>
      {lang==='pt'?'Teclado':'Keypad'}
    </div>
    <div className="keypad">
      {['1','2','3','4','5','6','7','8','9'].map(k => (
        <button key={k} onClick={() => onPress(k)}>{k}</button>
      ))}
      <button className="action" onClick={() => onPress('space')}>—</button>
      <button onClick={() => onPress('0')}>0</button>
      <button className="action" onClick={() => onPress('back')}><Icon name="arrow-left" size={18}/></button>
    </div>
  </div>
);

const PaymentScreen = ({ method, setMethod, onApprove, t, lang, summary }) => {
  const [timer, setTimer] = React.useState(180);
  React.useEffect(() => {
    const id = setInterval(() => setTimer(s => Math.max(0, s - 1)), 1000);
    return () => clearInterval(id);
  }, []);
  const mm = String(Math.floor(timer/60)).padStart(2,'0');
  const ss = String(timer%60).padStart(2,'0');
  const pct = (timer / 180) * 100;
  return (
    <div className="screen screen-enter">
      <div className="screen-body">
        <h1 className="screen-title">{t.payment}</h1>
        <p className="screen-sub">{t.paymentSub}</p>
        <div style={{display:'grid', gridTemplateColumns:'1fr 380px', gap:20}}>
          <div>
            <div style={{display:'flex', gap:10, marginBottom:14}}>
              {[
                { k:'pix',    label:t.pix,    icon:'qr',   desc:t.pixDesc },
                { k:'credit', label:t.credit, icon:'card', desc:lang==='pt'?'Maquininha ao lado':'Terminal beside' },
                { k:'debit',  label:t.debit,  icon:'card', desc:lang==='pt'?'Maquininha ao lado':'Terminal beside' },
              ].map(opt => (
                <button key={opt.k} onClick={() => setMethod(opt.k)}
                  className={`card selectable pay-tab ${method===opt.k?'selected':''}`}>
                  <div className="pay-tab-icon"><Icon name={opt.icon} size={22}/></div>
                  <div>
                    <div className="pay-tab-label">{opt.label}</div>
                    <div className="pay-tab-desc">{opt.desc}</div>
                  </div>
                </button>
              ))}
            </div>
            {method === 'pix' && (
              <div className="card pay-method" style={{padding:24}}>
                <div style={{display:'grid', gridTemplateColumns:'auto 1fr', gap:28, alignItems:'center'}}>
                  <div className="qr-frame" style={{animation:'staggerIn 380ms var(--ease-out)'}}>
                    <div className="qr-pattern"/>
                  </div>
                  <div>
                    <div style={{fontSize:13, fontWeight:700, color:'var(--text-muted)', textTransform:'uppercase', letterSpacing:'0.08em', marginBottom:6}}>{lang==='pt'?'Pagamento PIX':'PIX Payment'}</div>
                    <div style={{fontSize:32, fontWeight:800, color:'var(--primary-dark)', letterSpacing:'-0.02em', marginBottom:14, fontVariantNumeric:'tabular-nums'}}>
                      R$ {summary.total.toFixed(2).replace('.',',')}
                    </div>
                    <ol style={{margin:0, padding:'0 0 0 18px', color:'var(--neutral-700)', fontSize:13, lineHeight:1.7}}>
                      <li>{lang==='pt'?'Abra o app do seu banco':'Open your bank app'}</li>
                      <li>{lang==='pt'?'Escolha pagar com PIX › QR Code':'Pay with PIX › QR Code'}</li>
                      <li>{lang==='pt'?'Aponte a câmera para o código':'Point the camera at the code'}</li>
                    </ol>
                    <div className="pix-timer" style={{marginTop:14}}>
                      <span>{t.expiresIn}</span>
                      <div className="bar"><div className="bar-fill" style={{transform:`scaleX(${pct/100})`, transition:'transform 1s linear'}}/></div>
                      <span style={{fontWeight:700, color:'var(--neutral-900)'}}>{mm}:{ss}</span>
                    </div>
                    <div className="pay-status" style={{marginTop:12}}>
                      <div className="pulse"/>
                      {t.pixHint}
                      <button className="btn btn-secondary" style={{marginLeft:'auto', padding:'8px 14px', minHeight:0, fontSize:12}} onClick={onApprove}>
                        {lang==='pt'?'Simular':'Simulate'}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}
            {(method === 'credit' || method === 'debit') && (
              <div className="card pay-method" style={{padding:36, alignItems:'center', textAlign:'center'}}>
                <div style={{width:96, height:96, borderRadius:24, background:'var(--primary-50)', color:'var(--primary-dark)', display:'grid', placeItems:'center', margin:'0 auto 18px', animation:'idleFloat 2.4s ease-in-out infinite'}}>
                  <Icon name="card" size={48} stroke={1.8}/>
                </div>
                <div style={{fontSize:22, fontWeight:700, marginBottom:6}}>
                  {method==='credit' ? t.credit : t.debit}
                </div>
                <div style={{color:'var(--text-muted)', fontSize:14, marginBottom:18, maxWidth:380, marginLeft:'auto', marginRight:'auto'}}>
                  {t.cardHint}
                </div>
                <div className="pay-status" style={{maxWidth:340}}>
                  <div className="pulse"/>
                  {lang==='pt'?'Conectando com a maquininha':'Connecting to terminal'}
                  <button className="btn btn-secondary" style={{marginLeft:'auto', padding:'8px 14px', minHeight:0, fontSize:12}} onClick={onApprove}>
                    {lang==='pt'?'Simular':'Simulate'}
                  </button>
                </div>
              </div>
            )}
          </div>
          <SummaryCard summary={summary} t={t} lang={lang}/>
        </div>
      </div>
    </div>
  );
};

const SummaryCard = ({ summary, t, lang }) => (
  <div className="side-summary">
    <h3>{t.summary}</h3>
    <div className="summary-row"><span className="label">{t.route}</span><span className="value">{summary.route}</span></div>
    <div className="summary-row"><span className="label">{t.date}</span><span className="value">{summary.date}</span></div>
    <div className="summary-row"><span className="label">{t.departure}</span><span className="value">{summary.dep}</span></div>
    <div className="summary-row"><span className="label">{t.room}</span><span className="value">{summary.room}</span></div>
    <div className="summary-row"><span className="label">{t.passengers}</span><span className="value">1</span></div>
    <div className="summary-total">
      <span className="label">{t.totalCaps}</span>
      <span className="value">R$ {summary.total.toFixed(2).replace('.',',')}</span>
    </div>
  </div>
);

const ConfirmScreen = ({ booking, onReset, onPrint, t, lang }) => (
  <div className="screen screen-enter">
    <div className="confirm-screen">
      <div className="success-mark"><Icon name="check-big" size={64} stroke={3}/></div>
      <h1 className="confirm-title">{t.confirmTitle}</h1>
      <p className="confirm-sub">{t.confirmSub}</p>
      <div className="ticket-stub">
        <div className="ticket-section">
          <div className="lbl">{t.bookingCode}</div>
          <div className="val" style={{fontVariantNumeric:'tabular-nums'}}>{booking.code}</div>
        </div>
        <div className="ticket-divider"/>
        <div className="ticket-section">
          <div className="lbl">{t.route}</div>
          <div className="val">{booking.route}</div>
        </div>
        <div className="ticket-divider"/>
        <div className="ticket-section">
          <div className="lbl">{t.departure}</div>
          <div className="val">{booking.date} · {booking.dep}</div>
        </div>
        <div className="ticket-divider"/>
        <div className="ticket-section">
          <div className="lbl">{t.room}</div>
          <div className="val">{booking.room}</div>
        </div>
      </div>
      <div className="confirm-actions">
        <button className="btn btn-secondary" onClick={onPrint}>
          <Icon name="printer" size={20}/>{t.printAgain}
        </button>
        <button className="btn btn-primary" onClick={onReset}>
          <Icon name="check" size={20} stroke={3}/>{t.finish}
        </button>
      </div>
      <div className="confirm-reset">
        <span>{t.autoReset}</span>
        <div className="reset-bar"><div className="reset-bar-fill"/></div>
      </div>
    </div>
  </div>
);

Object.assign(window, { PassengerScreen, PaymentScreen, ConfirmScreen, SummaryCard });
