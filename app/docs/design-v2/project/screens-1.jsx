// Screens — Minimal redesign · Idle, City, Date, Trip, Room

const IdleScreen = ({ onStart, lang, setLang }) => (
  <div className="idle" onClick={onStart}>
    <div className="idle-top">
      <div className="idle-brand">
        <span className="dot"/>passagens express
      </div>
      <button
        className="pill"
        onClick={(e) => { e.stopPropagation(); setLang(lang === 'pt' ? 'en' : 'pt'); }}
      >
        <Icon name="globe" size={18}/>
        <span>{lang === 'pt' ? 'PT' : 'EN'}</span>
      </button>
    </div>
    <div className="idle-body">
      <h1 className="idle-wordmark">
        ferry<span className="accent">.</span>
      </h1>
      <p className="idle-tag">
        {lang === 'pt'
          ? 'Compre seu bilhete e embarque em menos de um minuto.'
          : 'Buy your ticket and board in under a minute.'}
      </p>
      <div className="idle-arrow"><Icon name="arrow-right" size={26} stroke={2.4}/></div>
      <div className="idle-tap">
        {lang === 'pt' ? 'Toque para começar' : 'Tap to start'}
      </div>
    </div>
    <div className="idle-foot">
      {lang === 'pt' ? 'Salvador · Terminal Marítimo' : 'Salvador · Maritime Terminal'}
    </div>
  </div>
);

// ============================================================
// City — list of destinations
// ============================================================
const CityScreen = ({ value, onPick, t, lang }) => (
  <>
    <h1 className="screen-title">{t.pickCity}</h1>
    <p className="screen-sub">{t.pickCitySub}</p>
    <div className="list stagger">
      {CITIES.map((c, i) => (
        <div
          key={c.id}
          className={`row ${value === c.id ? 'selected' : ''}`}
          style={{animationDelay: `${i * 30}ms`}}
          onClick={() => onPick(c.id)}
        >
          <div className="left">
            <div className="ico"><Icon name="pin" size={26} stroke={1.8}/></div>
            <div style={{minWidth:0}}>
              <div className="main">
                {c.name}
                {c.popular && <span className="tag">{t.popular}</span>}
              </div>
              <div className="sub">{c.desc} · {c.duration} {t.minutes}</div>
            </div>
          </div>
          <div className="right">
            <Icon name="arrow-right" size={22} stroke={2} className="chev"/>
          </div>
        </div>
      ))}
    </div>
  </>
);

// ============================================================
// Date — horizontal pill scroller
// ============================================================
const DateScreen = ({ value, onPick, t, lang }) => (
  <>
    <h1 className="screen-title">{t.pickDate}</h1>
    <p className="screen-sub">{t.pickDateSub}</p>
    <div className="date-row stagger">
      {DATES.map((d, i) => (
        <div
          key={d.iso}
          className={`date-pill ${value === d.iso ? 'selected' : ''} ${d.isToday?'today':''} ${d.isTomorrow?'tomorrow':''}`}
          style={{animationDelay: `${i * 25}ms`}}
          onClick={() => onPick(d.iso)}
        >
          <div className="dow">{lang === 'pt' ? d.dow_pt : d.dow_en}</div>
          <div className="day">{d.day}</div>
          <div className="mon">{lang === 'pt' ? d.mon_pt : d.mon_en}</div>
          {d.isToday && <div className="badge">{t.today}</div>}
          {d.isTomorrow && <div className="badge">{t.tomorrow}</div>}
        </div>
      ))}
    </div>
    <p style={{marginTop: 32, fontSize: 14, color: 'var(--ink-muted)'}}>
      {lang === 'pt'
        ? 'Arraste para ver mais datas. Vendemos com até 14 dias de antecedência.'
        : 'Swipe to see more dates. We sell tickets up to 14 days ahead.'}
    </p>
  </>
);

// ============================================================
// Trip — list of departure times
// ============================================================
const TripScreen = ({ city, date, value, onPick, onChangeDate, t, lang }) => {
  const c = CITIES.find(x => x.id === city);
  const dateData = DATES.find(d => d.iso === date);
  const dateIdx = DATES.findIndex(d => d.iso === date);
  const goPrev = () => dateIdx > 0 && onChangeDate(DATES[dateIdx - 1].iso);
  const goNext = () => dateIdx < DATES.length - 1 && onChangeDate(DATES[dateIdx + 1].iso);

  return (
    <>
      <h1 className="screen-title">{t.pickTrip}</h1>
      <p className="screen-sub">
        {c && <>Salvador → {c.name} · {c.duration} {t.minutes}</>}
      </p>

      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        marginBottom: 20,
        padding: '14px 18px',
        background: 'var(--paper-warm)',
        borderRadius: 16,
      }}>
        <button className="icon-btn" onClick={goPrev} disabled={dateIdx <= 0}
          style={{background:'white', opacity: dateIdx <= 0 ? 0.3 : 1}}>
          <Icon name="arrow-left" size={18}/>
        </button>
        <div style={{textAlign:'center'}}>
          <div style={{fontSize:11, fontWeight:700, letterSpacing:'0.14em', textTransform:'uppercase', color:'var(--ink-muted)'}}>
            {dateData && (lang === 'pt' ? dateData.dow_pt : dateData.dow_en)}
          </div>
          <div style={{fontSize:18, fontWeight:600, letterSpacing:'-0.01em', marginTop:2}}>
            {dateData && <>{dateData.day} {lang === 'pt' ? dateData.mon_pt : dateData.mon_en}</>}
          </div>
        </div>
        <button className="icon-btn" onClick={goNext} disabled={dateIdx >= DATES.length - 1}
          style={{background:'white', opacity: dateIdx >= DATES.length - 1 ? 0.3 : 1}}>
          <Icon name="arrow-right" size={18}/>
        </button>
      </div>

      <div className="list stagger">
        {TRIP_TIMES.map((trip, i) => {
          const arrival = (() => {
            if (!c) return '';
            const [h, m] = trip.dep.split(':').map(Number);
            const total = h * 60 + m + c.duration;
            return `${String(Math.floor(total/60)).padStart(2,'0')}:${String(total%60).padStart(2,'0')}`;
          })();
          const seatsLeft = trip.badge === 'almost-full' ? 6 : 30 + (i * 7) % 20;
          return (
            <div
              key={i}
              className={`row ${value === i ? 'selected' : ''}`}
              style={{animationDelay: `${i * 30}ms`}}
              onClick={() => onPick(i)}
            >
              <div className="left">
                <div className="ico">
                  <div style={{fontFamily:'var(--font-display)', fontSize:18, letterSpacing:'-0.02em', color: value === i ? 'white' : 'var(--ink)'}}>
                    {trip.dep}
                  </div>
                </div>
                <div style={{minWidth:0}}>
                  <div className="main" style={{fontFamily:'var(--font-display)', fontSize:30, fontWeight:400, letterSpacing:'-0.01em'}}>
                    {trip.dep} <span style={{color:'var(--ink-soft)', margin:'0 4px'}}>→</span> {arrival}
                    {trip.badge === 'express' && <span className="tag">{t.express}</span>}
                    {trip.badge === 'almost-full' && <span className="tag" style={{color:'#C24714'}}>{t.almostFull}</span>}
                  </div>
                  <div className="sub">
                    <strong style={{color:'var(--ink-2)', fontWeight:600}}>{seatsLeft}</strong> {t.seats} {t.available}
                  </div>
                </div>
              </div>
              <div className="right">
                <Icon name="arrow-right" size={22} stroke={2} className="chev"/>
              </div>
            </div>
          );
        })}
      </div>
    </>
  );
};

// ============================================================
// Room — list of cabin types with prices
// ============================================================
const ROOM_ICON_NAMES = {
  salao: 'lounge',
  panoramica: 'panoramic',
  externa: 'deck',
  vip: 'vip',
};

const SimpleRoomIcon = ({ kind }) => {
  const stroke = { stroke: 'currentColor', strokeWidth: 1.6, fill: 'none', strokeLinecap: 'round', strokeLinejoin: 'round' };
  switch (kind) {
    case 'lounge': return (
      <svg width="28" height="28" viewBox="0 0 24 24" {...stroke}>
        <rect x="3" y="8" width="18" height="9" rx="1.5"/>
        <path d="M3 12h18M8 8v9M16 8v9"/>
      </svg>
    );
    case 'panoramica': return (
      <svg width="28" height="28" viewBox="0 0 24 24" {...stroke}>
        <path d="M3 8a9 5 0 0 1 18 0v9H3z"/>
        <path d="M3 12h18M9 8v9M15 8v9"/>
      </svg>
    );
    case 'externa': return (
      <svg width="28" height="28" viewBox="0 0 24 24" {...stroke}>
        <path d="M2 17h20M4 17l2-10h12l2 10"/>
        <path d="M7 11h10M6 14h12"/>
        <circle cx="12" cy="5" r="1.5"/>
      </svg>
    );
    case 'vip': return (
      <svg width="28" height="28" viewBox="0 0 24 24" {...stroke}>
        <path d="M5 9l3 8h8l3-8-4 2-3-5-3 5z"/>
        <path d="M7 19h10"/>
      </svg>
    );
    default: return null;
  }
};

const RoomScreen = ({ city, value, onPick, t, lang }) => {
  const c = CITIES.find(x => x.id === city);
  return (
    <>
      <h1 className="screen-title">{t.pickRoom}</h1>
      <p className="screen-sub">
        {c && <>Salvador → {c.name}</>} · {t.pickRoomSub}
      </p>
      <div className="list stagger">
        {ROOMS.map((r, i) => {
          const left = r.capacity - r.taken;
          const sold = left <= 5;
          const full = left <= 0;
          return (
            <div
              key={r.id}
              className={`row ${value === r.id ? 'selected' : ''} ${full ? 'disabled' : ''}`}
              style={{animationDelay: `${i * 40}ms`}}
              onClick={() => !full && onPick(r.id)}
            >
              <div className="left">
                <div className="ico">
                  <SimpleRoomIcon kind={r.id === 'salao' ? 'lounge' : r.id === 'panoramica' ? 'panoramica' : r.id === 'externa' ? 'externa' : 'vip'}/>
                </div>
                <div style={{minWidth:0}}>
                  <div className="main">{lang === 'pt' ? r.name_pt : r.name_en}</div>
                  <div className="sub">
                    {lang === 'pt' ? r.desc_pt : r.desc_en}
                    {' · '}
                    {full
                      ? (lang === 'pt' ? 'Esgotado' : 'Sold out')
                      : sold
                        ? <span style={{color:'var(--accent)', fontWeight:600}}>{left} {t.seats} {t.available}</span>
                        : <>{left} {t.seats} {t.available}</>}
                  </div>
                </div>
              </div>
              <div className="right">
                <div className="price">R$ {r.price.toFixed(2).replace('.',',').replace(',00','')}</div>
                <Icon name="arrow-right" size={22} stroke={2} className="chev"/>
              </div>
            </div>
          );
        })}
      </div>
    </>
  );
};

Object.assign(window, { IdleScreen, CityScreen, DateScreen, TripScreen, RoomScreen });
