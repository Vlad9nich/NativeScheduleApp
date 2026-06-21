# Context — NativeScheduleApp

## Проект
Нативное Android-приложение на Kotlin + Jetpack Compose (`NativeScheduleApp`), переносящее функционал сайта kernelordexter.ru.

## Текущий этап: Полировка дизайна под сайт + Производительность

### Архитектура файлов
```
app/src/main/
├── assets/
│   ├── maps/
│   │   ├── floor_0.jpg — floor_4.jpg   # Склеенные карты этажей (ORB + Lanczos x2)
│   │   └── graph_data.json              # Граф: 142 узла, 144 ребра, 5 этажей
│   └── schedule_data.json               # Расписание с сервера
├── java/ru/kernelordexter/app/
│   ├── MainActivity.kt      # Расписание (683 строки) — live-статус, недели, дни, карточки пар
│   ├── MapActivity.kt       # Карта здания (757 строк) — floor tabs, pinch-zoom, A* маршрут
│   ├── PathFinder.kt        # A* алгоритм (187 строк) — Opus 4.6: IntArray, FloatMinHeap, корутины
│   └── ScheduleDataClasses.kt  # Модели JSON расписания
├── res/font/                # НУЖНО: добавить Manrope + Oswald .ttf файлы
└── res/values/styles.xml    # Theme.ScheduleApp
```

### Бренд-система сайта kernelordexter.ru
```
Цвета:
  brand-black:    #0a0a0a  (фон)
  brand-red:      #e50914  (акцент, кнопки, glow)
  brand-darkGray: #171717  (карточки)
  brand-midGray:  #262626  (вторичные элементы)
  brand-lightGray:#a3a3a3  (текст secondary)

Шрифты:
  Oswald (500, 700)   — заголовки, время, надписи uppercase
  Manrope (400-800)   — body text, кнопки, описания

Эффекты:
  glass-card:    background: rgba(23,23,23,0.7); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.05)
  red-glow:      box-shadow: 0 0 20px rgba(229,9,20,0.3)
  animated-bg:   radial-gradient(circle at 50% 0%, rgba(229,9,20,0.08), rgba(10,10,10,1) 50%)
  schedule-row:  slideIn 0.4s cubic-bezier(0.16, 1, 0.3, 1)
```

### Что уже сделано (Opus 4.6 сессия 2026-06-21)
1. ✅ Карта: 5 этажей склеены из 13 скриншотов, апскейл x2
2. ✅ Граф: 142 узла (100 кабинетов + 38 коридоров + 4 лестницы + вход), 144 ребра, BFS 142/142
3. ✅ PathFinder.kt: A* с FloatMinHeap, IntArray, корутины (Opus 4.6 архитектура)
4. ✅ MapActivity.kt: реальные данные, floor tabs, glow-маршрут, transition markers
5. ✅ build.gradle.kts: kotlinx-coroutines-android

### Что нужно сделать (текущая задача)
1. **Шрифты**: Скачать Manrope + Oswald .ttf → res/font/ → обновить FontFamily
2. **Glass-эффект**: Полупрозрачные карточки с border rgba(255,255,255,0.05)
3. **Animated BG**: Radial gradient красного цвета сверху экрана
4. **Red glow**: Тени на кнопках и акцентных элементах
5. **Slide анимации**: Staggered slideIn для строк расписания
6. **Производительность**: Кэшировать SimpleDateFormat, добавить key в LazyColumn

### Зависимости (build.gradle.kts)
- compileSdk 34, minSdk 24
- Jetpack Compose BOM 2023.03.00
- Material3
- Gson 2.10.1
- kotlinx-coroutines-android 1.7.3
- Kotlin compiler extension 1.5.0

### Git
- Repo: github.com/Vlad9nich/NativeScheduleApp (main branch)
- Последний коммит: 1f80df0 "refactor: complete rewrite of map navigation engine"
