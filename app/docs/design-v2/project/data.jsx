// Static data: cities, trips, copy
const I18N = {
  pt: {
    tapToStart: 'Toque para começar',
    pickCity: 'Para qual cidade você vai?',
    pickCitySub: 'Escolha o seu destino',
    popular: 'Popular',
    pickDate: 'Quando deseja viajar?',
    pickDateSub: 'Selecione a data da partida',
    today: 'Hoje',
    tomorrow: 'Amanhã',
    pickTrip: 'Escolha a viagem',
    pickTripSub: 'Travessias disponíveis',
    duration: 'Duração',
    seats: 'lugares',
    available: 'disponíveis',
    almostFull: 'Quase cheio',
    express: 'Expresso',
    prevDay: 'Dia anterior',
    nextDay: 'Próximo dia',
    pickRoom: 'Escolha o cômodo',
    pickRoomSub: 'Selecione onde deseja viajar na embarcação',
    passengerData: 'Seus dados',
    passengerDataSub: 'Necessários para emissão do bilhete',
    name: 'Nome completo',
    cpf: 'CPF',
    phone: 'Celular',
    birth: 'Data de nascimento',
    payment: 'Pagamento',
    paymentSub: 'Confira o resumo e escolha como pagar',
    pix: 'PIX',
    pixDesc: 'Aponte a câmera do celular',
    pixHint: 'Aguardando confirmação do banco...',
    credit: 'Crédito',
    debit: 'Débito',
    cardHint: 'Insira ou aproxime o cartão na maquininha ao lado',
    expiresIn: 'Expira em',
    confirmTitle: 'Pagamento aprovado!',
    confirmSub: 'Seu bilhete foi impresso. Boa viagem!',
    bookingCode: 'Código',
    departure: 'Partida',
    room: 'Cômodo',
    passengers: 'Passageiros',
    printAgain: 'Imprimir novamente',
    finish: 'Concluir',
    autoReset: 'Reiniciando em',
    back: 'Voltar',
    continue: 'Continuar',
    pay: 'Pagar',
    needHelp: 'Precisa de ajuda? Procure um atendente.',
    minutes: 'min',
    total: 'Total',
    totalCaps: 'TOTAL',
    summary: 'Resumo',
    route: 'Rota',
    date: 'Data',
    selected: 'Selecionado',
    occupied: 'Ocupado',
    free: 'Livre',
  },
  en: {
    tapToStart: 'Tap to start',
    pickCity: 'Which city are you going to?',
    pickCitySub: 'Pick your destination',
    popular: 'Popular',
    pickDate: 'When are you traveling?',
    pickDateSub: 'Select the departure date',
    today: 'Today',
    tomorrow: 'Tomorrow',
    pickTrip: 'Choose your trip',
    pickTripSub: 'Available crossings',
    duration: 'Duration',
    seats: 'seats',
    available: 'available',
    almostFull: 'Almost full',
    express: 'Express',
    prevDay: 'Previous day',
    nextDay: 'Next day',
    pickRoom: 'Choose your room',
    pickRoomSub: 'Pick where you\'d like to travel on the boat',
    passengerData: 'Your details',
    passengerDataSub: 'Required to issue your ticket',
    name: 'Full name',
    cpf: 'Tax ID (CPF)',
    phone: 'Mobile',
    birth: 'Date of birth',
    payment: 'Payment',
    paymentSub: 'Review your order and choose a payment method',
    pix: 'PIX',
    pixDesc: 'Scan with your phone',
    pixHint: 'Waiting for bank confirmation…',
    credit: 'Credit',
    debit: 'Debit',
    cardHint: 'Insert or tap your card on the terminal beside the kiosk',
    expiresIn: 'Expires in',
    confirmTitle: 'Payment approved!',
    confirmSub: 'Your ticket has been printed. Have a great trip!',
    bookingCode: 'Code',
    departure: 'Departure',
    room: 'Room',
    passengers: 'Passengers',
    printAgain: 'Print again',
    finish: 'Finish',
    autoReset: 'Restarting in',
    back: 'Back',
    continue: 'Continue',
    pay: 'Pay',
    needHelp: 'Need help? Ask an attendant.',
    minutes: 'min',
    total: 'Total',
    totalCaps: 'TOTAL',
    summary: 'Summary',
    route: 'Route',
    date: 'Date',
    selected: 'Selected',
    occupied: 'Taken',
    free: 'Free',
  },
};

// Cities — destinations from this kiosk's home port
const CITIES = [
  { id: 'mar-grande', name: 'Mar Grande',         desc: 'Vera Cruz · Ilha de Itaparica',     duration: 35,  hue: 200, popular: true },
  { id: 'itaparica',  name: 'Itaparica',          desc: 'Ilha de Itaparica',                 duration: 50,  hue: 210, popular: true },
  { id: 'morro',      name: 'Morro de S. Paulo',  desc: 'Cairu · Bahia',                     duration: 150, hue: 190, popular: true },
  { id: 'boipeba',    name: 'Boipeba',            desc: 'Ilha de Boipeba · Cairu',           duration: 180, hue: 195, popular: false },
  { id: 'paqueta',    name: 'Paquetá',            desc: 'Baía de Guanabara',                 duration: 70,  hue: 230, popular: false },
  { id: 'ilha-grande',name: 'Ilha Grande',        desc: 'Angra dos Reis · RJ',               duration: 90,  hue: 215, popular: true },
  { id: 'guaruja',    name: 'Guarujá',            desc: 'Travessia de Santos',               duration: 8,   hue: 205, popular: false },
  { id: 'jurere',     name: 'Jurerê',             desc: 'Florianópolis · SC',                duration: 25,  hue: 220, popular: false },
];

// Rooms / sections of the boat
const ROOMS = [
  { id: 'salao',     name_pt: 'Salão Principal',  name_en: 'Main Lounge',    desc_pt: 'Cadeiras estofadas · Ar condicionado', desc_en: 'Cushioned seats · A/C',         price: 35.00, capacity: 80, taken: 18, icon: 'lounge' },
  { id: 'panoramica',name_pt: 'Cabine Panorâmica',name_en: 'Panoramic Cabin',desc_pt: 'Vista 360° · Lugares preferenciais',  desc_en: 'Panoramic view · Premium seats',price: 65.00, capacity: 24, taken: 22, icon: 'panoramic' },
  { id: 'externa',   name_pt: 'Área Externa',     name_en: 'Open Deck',      desc_pt: 'Ao ar livre · Vista para o mar',      desc_en: 'Open air · Sea view',           price: 28.00, capacity: 40, taken: 12, icon: 'deck' },
  { id: 'vip',       name_pt: 'Lounge VIP',       name_en: 'VIP Lounge',     desc_pt: 'Bebida cortesia · Acesso prioritário',desc_en: 'Welcome drink · Priority boarding',price: 95.00, capacity: 12, taken: 4,  icon: 'vip' },
];

const TRIP_TIMES = [
  { dep: '08:00', badge: null },
  { dep: '09:30', badge: 'express' },
  { dep: '11:00', badge: null },
  { dep: '13:30', badge: null },
  { dep: '15:00', badge: 'almost-full' },
  { dep: '16:30', badge: 'express' },
  { dep: '18:00', badge: null },
];

function generateDates(count = 14) {
  const out = [];
  const base = new Date(2026, 4, 6);
  const dows_pt = ['DOM','SEG','TER','QUA','QUI','SEX','SÁB'];
  const dows_en = ['SUN','MON','TUE','WED','THU','FRI','SAT'];
  const months_pt = ['JAN','FEV','MAR','ABR','MAI','JUN','JUL','AGO','SET','OUT','NOV','DEZ'];
  const months_en = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];
  for (let i = 0; i < count; i++) {
    const d = new Date(base);
    d.setDate(base.getDate() + i);
    out.push({
      iso: d.toISOString().slice(0, 10),
      day: d.getDate(),
      dow_pt: dows_pt[d.getDay()],
      dow_en: dows_en[d.getDay()],
      mon_pt: months_pt[d.getMonth()],
      mon_en: months_en[d.getMonth()],
      isToday: i === 0,
      isTomorrow: i === 1,
    });
  }
  return out;
}

const DATES = generateDates();

Object.assign(window, { I18N, CITIES, TRIP_TIMES, DATES, ROOMS });
