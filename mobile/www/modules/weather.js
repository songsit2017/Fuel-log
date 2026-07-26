const WEATHER_CODES = Object.freeze({
  0: 'ท้องฟ้าแจ่มใส', 1: 'แจ่มใสเป็นส่วนใหญ่', 2: 'มีเมฆบางส่วน', 3: 'ครึ้ม',
  45: 'มีหมอก', 48: 'มีหมอกน้ำค้างแข็ง', 51: 'ฝนปรอยเบา', 53: 'ฝนปรอย',
  55: 'ฝนปรอยหนัก', 61: 'ฝนเบา', 63: 'ฝนปานกลาง', 65: 'ฝนหนัก',
  71: 'หิมะเบา', 73: 'หิมะปานกลาง', 75: 'หิมะหนัก', 80: 'ฝนซู่เบา',
  81: 'ฝนซู่ปานกลาง', 82: 'ฝนซู่หนัก', 95: 'พายุฝนฟ้าคะนอง'
});

export function getPosition(options = {}) {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) return reject(new Error('อุปกรณ์ไม่รองรับตำแหน่ง'));
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: false,
      timeout: 8000,
      maximumAge: 300000,
      ...options
    });
  });
}

export async function captureWeather(fetchImpl = fetch) {
  const position = await getPosition();
  const { latitude, longitude, accuracy } = position.coords;
  const url = new URL('https://api.open-meteo.com/v1/forecast');
  url.search = new URLSearchParams({
    latitude: latitude.toFixed(5),
    longitude: longitude.toFixed(5),
    current: 'temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m',
    timezone: 'auto'
  });
  const response = await fetchImpl(url);
  if (!response.ok) throw new Error(`Open-Meteo HTTP ${response.status}`);
  const result = await response.json();
  const current = result.current || {};
  return {
    provider: 'Open-Meteo',
    capturedAt: new Date().toISOString(),
    latitude: Number(latitude.toFixed(5)),
    longitude: Number(longitude.toFixed(5)),
    accuracyMeters: Math.round(accuracy || 0),
    timezone: result.timezone || '',
    temperatureC: current.temperature_2m ?? null,
    apparentTemperatureC: current.apparent_temperature ?? null,
    humidityPercent: current.relative_humidity_2m ?? null,
    precipitationMm: current.precipitation ?? null,
    windSpeedKmh: current.wind_speed_10m ?? null,
    weatherCode: current.weather_code ?? null,
    description: WEATHER_CODES[current.weather_code] || 'ไม่ทราบสภาพอากาศ'
  };
}

export function weatherSummary(weather) {
  if (!weather) return '';
  const temperature = weather.temperatureC == null ? '' : `${weather.temperatureC}°C`;
  return [weather.description, temperature].filter(Boolean).join(' • ');
}
