// Screens: Idle, City, Date, Trip, Room
const IdleScreen = ({ onStart, t }) => (
  <div className="idle-screen" onClick={onStart}>
    <svg className="idle-waves" viewBox="0 0 1280 200" preserveAspectRatio="none">
      <path d="M0,100 Q160,40 320,100 T640,100 T960,100 T1280,100 V200 H0 Z" fill="white">
        <animate attributeName="d"
          values="M0,100 Q160,40 320,100 T640,100 T960,100 T1280,100 V200 H0 Z;
                  M0,100 Q160,160 320,100 T640,100 T960,100 T1280,100 V200 H0 Z;
                  M0,100 Q160,40 320,100 T640,100 T960,100 T1280,100 V200 H0 Z"
          dur="6s" repeatCount="indefinite"/>
      </path>
    </svg>
    <div className="idle-content">
      <div className="idle-logo">
        <Icon name="ferry" size={52} stroke={2}/>
      </div>
      <h1 className="idle-title">Passagens Express</h1>
      <p className="idle-sub">Compre seu bilhete em menos de 1 minuto · Buy your ticket in under a minute</p>
      <div className="idle-tap">
        <Icon name="sparkle" size={22} stroke={2.5}/>
        {t.tapToStart}
      </div>
    </div>
  </div>
);

// Iconographic placeholder for each city — striped + monogram, no fake illustrations
const CityImage = ({ city }) => (
  <div className="city-image" style={{
    background: `linear-gradient(135deg, oklch(0.78 0.10 ${city.hue}) 0%, oklch(0.42 0.18 ${city.hue}) 100%)`
  }}>
    <div className="stripes"/>
    <div className="city-mono">{city.name.split(' ').map(w => w[0]).slice(0,2).join('')}</div>
    <svg className="city-wave" viewBox="0 0 200 40" preserveAspectRatio="none">
      <path d="M0,20 Q25,5 50,20 T100,20 T150,20 T200,20 V40 H0 Z" fill="rgba(255,255,255,0.18)"/>
      <path d="M0,28 Q25,15 50,28 T100,28 T150,28 T200,28 V40 H0 Z" fill="rgba(255,255,255,0.10)"/>
    </svg>
  </div>
);

const CityScreen = ({ value, onPick, t, lang }) => (
  <div className="screen screen-enter">
    <div className="screen-body">
      <h1 className="screen-title">{t.pickCity}</h1>
      <p className="screen-sub">{t.pickCitySub}</p>
      <div className="city-grid stagger">
        {CITIES.map((c, i) => (
          <div
            key={c.id}
            className={`card selectable city-card ${value === c.id ? 'selected' : ''}`}
            style={{animationDelay: `${i * 40}ms`}}
            onClick={() => onPick(c.id)}
          >
            <CityImage city={c}/>
            {c.popular && (
              <div className="dest-popular">
                <Icon name="sparkle" size={11} stroke={2.5}/>{t.popular}
              </div>
            )}
            <div className="city-info">
              <div className="city-name">{c.name}</div>
              <div className="city-desc">{c.desc}</div>
              <div className="city-meta">
                <span className="row" style={{gap:5}}><Icon name="clock" size={13}/>{c.duration} {t.minutes}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
);

const DateScreen = ({ value, onPick, t, lang }) => (
  <div className="screen screen-enter">
    <div className="screen-body">
      <h1 className="screen-title">{t.pickDate}</h1>
      <p className="screen-sub">{t.pickDateSub}</p>
      <div className="date-row">
        {DATES.map((d, i) => (
          <div
            key={d.iso}
            className={`date-pill ${value === d.iso ? 'selected' : ''} ${d.isToday ? 'today' : ''}`}
            onClick={() => onPick(d.iso)}
            style={{opacity:0, animation:`staggerIn 380ms ${i*30}ms var(--ease-out) forwards`}}
          >
            <div className="dow">{lang==='pt'?d.dow_pt:d.dow_en}</div>
            <div className="day">{d.day}</div>
            <div className="mon">{lang==='pt'?d.mon_pt:d.mon_en}</div>
            {d.isToday && <div className="pill-tag">{t.today}</div>}
            {d.isTomorrow && <div className="pill-tag">{t.tomorrow}</div>}
          </div>
        ))}
      </div>
    </div>
  </div>
);

const TripScreen = ({ city, date, value, onPick, onChangeDate, t, lang }) => {
  const c = CITIES.find(x => x.id === city);
  const dateData = DATES.find(d => d.iso === date);
  const dateIdx = DATES.findIndex(d => d.iso === date);
  const goPrev = () => dateIdx > 0 && onChangeDate(DATES[dateIdx - 1].iso);
  const goNext = () => dateIdx < DATES.length - 1 && onChangeDate(DATES[dateIdx + 1].iso);
  return (
    <div className="screen screen-enter">
      <div className="screen-body">
        <div className="trip-header">
          <div>
            <h1 className="screen-title">{t.pickTrip}</h1>
            <p className="screen-sub">
              {c && <>Salvador <Icon name="arrow-right" size={13} stroke={2.5}/> {c.name} · {c.duration} {t.minutes}</>}
            </p>
          </div>
          <div className="date-switcher">
            <button className="ds-btn" onClick={goPrev} disabled={dateIdx <= 0}>
              <Icon name="arrow-left" size={18}/>
            </button>
            <div className="ds-display">
              <div className="ds-dow">{dateData && (lang==='pt'?dateData.dow_pt:dateData.dow_en)}</div>
              <div className="ds-date">{dateData && <>{dateData.day} {lang==='pt'?dateData.mon_pt:dateData.mon_en}</>}</div>
            </div>
            <button className="ds-btn" onClick={goNext} disabled={dateIdx >= DATES.length - 1}>
              <Icon name="arrow-right" size={18}/>
            </button>
          </div>
        </div>
        <div className="trip-list stagger">
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
                className={`card selectable trip-card ${value === i ? 'selected' : ''}`}
                style={{animationDelay:`${i*50}ms`}}
                onClick={() => onPick(i)}
              >
                <div>
                  {trip.badge === 'express' && <span className="trip-badge express">{t.express}</span>}
                  {trip.badge === 'almost-full' && <span className="trip-badge almost-full">{t.almostFull}</span>}
                  <div className="trip-time">{trip.dep}</div>
                  <div className="trip-stop">Salvador</div>
                </div>
                <div className="trip-route-line">
                  <Icon name="ferry" size={20} stroke={2}/>
                  <div className="trip-route-bar"/>
                  <div>{c?.duration} {t.minutes}</div>
                </div>
                <div className="trip-arrival">
                  <div className="trip-time">{arrival}</div>
                  <div className="trip-stop">{c && c.name}</div>
                </div>
                <div className="trip-meta-col">
                  <div><strong>{seatsLeft}</strong> {t.seats}</div>
                  <div>{t.available}</div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

// Room icons
const RoomIcon = ({ kind, size = 64 }) => {
  const stroke = { stroke: 'currentColor', strokeWidth: 1.6, fill: 'none', strokeLinecap: 'round', strokeLinejoin: 'round' };
  const w = size, h = size;
  switch (kind) {
    case 'lounge': return (
      <svg width={w} height={h} viewBox="0 0 64 64" {...stroke}>
        <rect x="10" y="20" width="44" height="22" rx="3"/>
        <path d="M10 30h44M22 20v22M42 20v22"/>
        <path d="M14 42v6M50 42v6"/>
      </svg>
    );
    case 'panoramic': return (
      <svg width={w} height={h} viewBox="0 0 64 64" {...stroke}>
        <path d="M8 18a24 14 0 0 1 48 0v22H8z"/>
        <path d="M8 28h48"/>
        <path d="M22 18v22M42 18v22"/>
        <circle cx="32" cy="46" r="2" fill="currentColor"/>
      </svg>
    );
    case 'deck': return (
      <svg width={w} height={h} viewBox="0 0 64 64" {...stroke}>
        <path d="M6 44h52M10 44l4-26h36l4 26"/>
        <path d="M16 28h32M14 36h36"/>
        <circle cx="32" cy="14" r="4"/>
      </svg>
    );
    case 'vip': return (
      <svg width={w} height={h} viewBox="0 0 64 64" {...stroke}>
        <path d="M14 22l8 18h20l8-18-10 6-8-12-8 12z"/>
        <path d="M18 46h28"/>
        <circle cx="14" cy="22" r="2" fill="currentColor"/>
        <circle cx="50" cy="22" r="2" fill="currentColor"/>
      </svg>
    );
    default: return null;
  }
};

const RoomScreen = ({ city, value, onPick, t, lang }) => {
  const c = CITIES.find(x => x.id === city);
  return (
    <div className="screen screen-enter">
      <div className="screen-body">
        <h1 className="screen-title">{t.pickRoom}</h1>
        <p className="screen-sub">{t.pickRoomSub} · {c && <>Salvador → {c.name}</>}</p>
        <div className="room-grid stagger">
          {ROOMS.map((r, i) => {
            const left = r.capacity - r.taken;
            const sold = left <= 5;
            const full = left <= 0;
            return (
              <div
                key={r.id}
                className={`card selectable room-card ${value === r.id ? 'selected' : ''} ${full ? 'taken-room' : ''}`}
                style={{animationDelay:`${i*60}ms`}}
                onClick={() => !full && onPick(r.id)}
              >
                <div className="room-icon"><RoomIcon kind={r.icon} size={56}/></div>
                <div className="room-body">
                  <div className="room-name">{lang==='pt'?r.name_pt:r.name_en}</div>
                  <div className="room-desc">{lang==='pt'?r.desc_pt:r.desc_en}</div>
                  <div className="room-meta">
                    <div className="room-cap">
                      <span className={`cap-dot ${sold?'low':''} ${full?'gone':''}`}/>
                      {full
                        ? (lang==='pt'?'Esgotado':'Sold out')
                        : <><strong>{left}</strong> {t.seats} {t.available}</>}
                    </div>
                  </div>
                </div>
                <div className="room-price">
                  <div className="amount">R$ {r.price.toFixed(2).replace('.',',')}</div>
                  <div className="lbl">{lang==='pt'?'por pessoa':'per person'}</div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

Object.assign(window, { IdleScreen, CityScreen, DateScreen, TripScreen, RoomScreen, CityImage, RoomIcon });
