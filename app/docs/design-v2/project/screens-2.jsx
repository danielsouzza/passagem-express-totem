// Screens — Minimal redesign · Passenger, Payment, Confirmation

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
    <>
      <h1 className="screen-title">{t.passengerData}</h1>
      <p className="screen-sub">{t.passengerDataSub}</p>

      <div className="form">
        {fields.map(f => (
          <div
            key={f.key}
            className={`field ${f.full ? 'full' : ''} ${value[f.key] ? 'filled' : ''}`}
          >
            <label>{f.label}</label>
            <input
              value={value[f.key] || ''}
              onFocus={() => setFocusKey(f.key)}
              onChange={(e) => onChange({ ...value, [f.key]: f.mask ? formatVal(f.key, e.target.value) : e.target.value })}
              inputMode={f.mask ? 'numeric' : 'text'}
            />
          </div>
        ))}
      </div>

      <div style={{
        marginTop: 24,
        padding: '14px 18px',
        background: 'var(--paper-warm)',
        borderRadius: 14,
        fontSize: 13,
        color: 'var(--ink-muted)',
        display: 'flex', alignItems: 'center', gap: 10,
      }}>
        <div style={{
          width: 8, height: 8, borderRadius: '50%',
          background: 'var(--accent)', flexShrink: 0,
        }}/>
        {lang === 'pt'
          ? 'Seus dados são usados apenas para emissão e validação do bilhete.'
          : 'Your details are used only to issue and validate your ticket.'}
      </div>

      <div style={{marginTop: 24}}>
        <div style={{fontSize:11, fontWeight:700, letterSpacing:'0.14em', textTransform:'uppercase', color:'var(--ink-muted)', marginBottom:12}}>
          {lang === 'pt' ? 'Teclado' : 'Keypad'}
        </div>
        <div className="keypad">
          {['1','2','3','4','5','6','7','8','9'].map(k => (
            <button key={k} onClick={() => {
              const cur = value[focusKey] || '';
              const isMasked = ['cpf','birth','phone'].includes(focusKey);
              const next = cur + k;
              onChange({ ...value, [focusKey]: isMasked ? formatVal(focusKey, next) : next });
            }}>{k}</button>
          ))}
          <button onClick={() => {
            const cur = value[focusKey] || '';
            onChange({ ...value, [focusKey]: cur + ' ' });
          }}>—</button>
          <button onClick={() => {
            const cur = value[focusKey] || '';
            const isMasked = ['cpf','birth','phone'].includes(focusKey);
            const next = cur + '0';
            onChange({ ...value, [focusKey]: isMasked ? formatVal(focusKey, next) : next });
          }}>0</button>
          <button onClick={() => {
            const cur = value[focusKey] || '';
            const next = cur.slice(0, -1);
            const isMasked = ['cpf','birth','phone'].includes(focusKey);
            onChange({ ...value, [focusKey]: isMasked ? formatVal(focusKey, next) : next });
          }}><Icon name="arrow-left" size={18}/></button>
        </div>
      </div>
    </>
  );
};

// ============================================================
// Payment
// ============================================================
const PaymentScreen = ({ method, setMethod, onApprove, t, lang, summary }) => {
  const [timer, setTimer] = React.useState(180);
  React.useEffect(() => {
    const id = setInterval(() => setTimer(s => Math.max(0, s - 1)), 1000);
    return () => clearInterval(id);
  }, []);
  const mm = String(Math.floor(timer/60)).padStart(2,'0');
  const ss = String(timer%60).padStart(2,'0');

  return (
    <>
      <h1 className="screen-title">{t.payment}</h1>
      <p className="screen-sub">{t.paymentSub}</p>

      <div className="summary">
        <h3>{t.summary}</h3>
        <div className="summary-row"><span className="l">{t.route}</span><span className="r">{summary.route}</span></div>
        <div className="summary-row"><span className="l">{t.date}</span><span className="r">{summary.date} · {summary.dep}</span></div>
        <div className="summary-row"><span className="l">{t.room}</span><span className="r">{summary.room}</span></div>
        <div className="summary-total">
          <span className="l">{t.totalCaps}</span>
          <span className="r">R$ {summary.total.toFixed(2).replace('.',',')}</span>
        </div>
      </div>

      <div className="pay-tabs">
        {[
          { k:'pix',    label:t.pix,    icon:'qr',   desc: lang==='pt'?'Aponte a câmera':'Scan with phone' },
          { k:'credit', label:t.credit, icon:'card', desc: lang==='pt'?'Maquininha ao lado':'Card terminal' },
          { k:'debit',  label:t.debit,  icon:'card', desc: lang==='pt'?'Maquininha ao lado':'Card terminal' },
        ].map(opt => (
          <button
            key={opt.k}
            onClick={() => setMethod(opt.k)}
            className={`pay-tab ${method === opt.k ? 'selected' : ''}`}
          >
            <div className="pti"><Icon name={opt.icon} size={20}/></div>
            <div style={{minWidth:0}}>
              <div className="ptl">{opt.label}</div>
              <div className="ptd">{opt.desc}</div>
            </div>
          </button>
        ))}
      </div>

      {method === 'pix' && (
        <div className="pay-panel">
          <div className="qr-card"><div className="qr-bits"/></div>
          <div className="pay-info">
            <div className="amount">
              <span>R$</span>{summary.total.toFixed(2).replace('.',',')}
            </div>
            <ul className="steps">
              <li>{lang==='pt'?'Abra o app do seu banco':'Open your bank app'}</li>
              <li>{lang==='pt'?'Escolha pagar com PIX · QR Code':'Pay with PIX · QR Code'}</li>
              <li>{lang==='pt'?'Aponte a câmera para o código':'Point the camera at the code'}</li>
            </ul>
            <div className="pay-status">
              <div className="pulse"/>
              {t.pixHint}
              <span style={{fontVariantNumeric:'tabular-nums', fontWeight:600}}>{mm}:{ss}</span>
              <button className="simulate" onClick={onApprove}>
                {lang==='pt'?'Simular':'Simulate'}
              </button>
            </div>
          </div>
        </div>
      )}

      {(method === 'credit' || method === 'debit') && (
        <div className="card-prompt">
          <div className="ico"><Icon name="card" size={36} stroke={1.6}/></div>
          <div className="amount">R$ {summary.total.toFixed(2).replace('.',',')}</div>
          <p className="hint">{t.cardHint}</p>
          <div className="pay-status" style={{justifyContent:'center', border:'none'}}>
            <div className="pulse"/>
            {lang==='pt'?'Conectando com a maquininha…':'Connecting to terminal…'}
            <button className="simulate" onClick={onApprove}>
              {lang==='pt'?'Simular':'Simulate'}
            </button>
          </div>
        </div>
      )}
    </>
  );
};

// ============================================================
// Confirmation — match the reference image
// ============================================================
const ConfirmQR = () => (
  // Simple stylized QR-ish square — pure shape, no text
  <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
    <rect x="6" y="6" width="20" height="20" rx="2" fill="none" stroke="currentColor" strokeWidth="3"/>
    <rect x="12" y="12" width="8" height="8" fill="currentColor"/>
    <rect x="74" y="6" width="20" height="20" rx="2" fill="none" stroke="currentColor" strokeWidth="3"/>
    <rect x="80" y="12" width="8" height="8" fill="currentColor"/>
    <rect x="6" y="74" width="20" height="20" rx="2" fill="none" stroke="currentColor" strokeWidth="3"/>
    <rect x="12" y="80" width="8" height="8" fill="currentColor"/>
    {/* Random bits in middle */}
    <rect x="34" y="6" width="6" height="6" fill="currentColor"/>
    <rect x="44" y="6" width="6" height="6" fill="currentColor"/>
    <rect x="54" y="14" width="6" height="6" fill="currentColor"/>
    <rect x="34" y="22" width="6" height="6" fill="currentColor"/>
    <rect x="54" y="34" width="6" height="6" fill="currentColor"/>
    <rect x="34" y="34" width="6" height="6" fill="currentColor"/>
    <rect x="44" y="44" width="6" height="6" fill="currentColor"/>
    <rect x="64" y="44" width="6" height="6" fill="currentColor"/>
    <rect x="34" y="54" width="6" height="6" fill="currentColor"/>
    <rect x="54" y="54" width="6" height="6" fill="currentColor"/>
    <rect x="74" y="54" width="6" height="6" fill="currentColor"/>
    <rect x="84" y="54" width="6" height="6" fill="currentColor"/>
    <rect x="44" y="64" width="6" height="6" fill="currentColor"/>
    <rect x="74" y="64" width="6" height="6" fill="currentColor"/>
    <rect x="34" y="74" width="6" height="6" fill="currentColor"/>
    <rect x="64" y="74" width="6" height="6" fill="currentColor"/>
    <rect x="84" y="74" width="6" height="6" fill="currentColor"/>
    <rect x="44" y="84" width="6" height="6" fill="currentColor"/>
    <rect x="64" y="84" width="6" height="6" fill="currentColor"/>
    <rect x="74" y="84" width="6" height="6" fill="currentColor"/>
  </svg>
);

const ConfirmScreen = ({ booking, onReset, t, lang }) => (
  <div className="screen">
    <div className="top-bar">
      <div className="step-counter" style={{color:'var(--accent)'}}>
        <Icon name="check" size={16} stroke={3}/>
        {lang === 'pt' ? 'Pedido aprovado' : 'Order approved'}
      </div>
      <div style={{fontSize: 13, color: 'var(--ink-muted)', fontWeight: 600, letterSpacing: '0.06em'}}>
        {lang === 'pt' ? 'Reinício em 30s' : 'Resets in 30s'}
      </div>
    </div>

    <div className="confirm">
      <div className="confirm-check"><Icon name="check" size={48} stroke={3}/></div>

      <h1 className="confirm-title">
        {lang === 'pt' ? 'Pedido confirmado!' : 'Order confirmed!'}
      </h1>
      <p className="confirm-sub">
        {lang === 'pt'
          ? 'Seu bilhete foi emitido. Mostre o QR no embarque.'
          : 'Your ticket has been issued. Show the QR when boarding.'}
      </p>

      <div className="confirm-qr" style={{color:'var(--ink)'}}>
        <ConfirmQR/>
      </div>

      <div className="confirm-hint">
        {lang === 'pt' ? 'Escaneie para receber o recibo' : 'Scan to get your receipt'}
      </div>
      <div className="confirm-or">{lang === 'pt' ? 'ou' : 'or'}</div>
      <div className="confirm-print-row">
        <span>{lang === 'pt' ? 'toque' : 'tap'}</span>
        <button className="pico" aria-label="Print" style={{
          width: 44, height: 44, borderRadius: 12,
          border: '1.5px solid var(--accent)',
          color: 'var(--accent)',
          display: 'grid', placeItems: 'center',
          position: 'relative',
        }}>
          <Icon name="printer" size={20} stroke={1.8}/>
        </button>
        <span>{lang === 'pt' ? 'para imprimir' : 'to print receipt'}</span>
      </div>

      <div className="confirm-code">{booking.code}</div>

      <button className="confirm-cta" onClick={onReset}>
        {lang === 'pt' ? 'Nova compra' : 'New order'}
      </button>
    </div>
  </div>
);

Object.assign(window, { PassengerScreen, PaymentScreen, ConfirmScreen });
