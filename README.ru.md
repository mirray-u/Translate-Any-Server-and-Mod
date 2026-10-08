# Translate Any Server & Mod

**Перевод текстов Minecraft: предметов, чата, модов и серверов — через Google, без API-ключа и локальной нейросети.**

[Скачать мод](https://github.com/mirray-u/Translate-Any-Server-and-Mod/releases/latest) · [English](README.md) · [Сообщить об ошибке](https://github.com/mirray-u/Translate-Any-Server-and-Mod/issues)

**Minecraft Java 26.2 · Fabric · Бесплатно · Открытый код · MIT**

Доработанная версия [SimpleTranslate от baokaixina](https://github.com/baokaixina/SimpleTranslate), которую развивает [mirray-u](https://github.com/mirray-u). Переводит отображаемые клиентом тексты в одиночной игре и на серверах. Ниже — реальные примеры английский → русский / китайский.

## Возможности

- Названия, описания и характеристики предметов, всплывающие подсказки.
- Чат и сообщения NPC: автоматически или по кнопке.
- Диалоги Wynncraft внутри родной коричневой рамки.
- Имена NPC и сущностей, текстовые сущности и таблички.
- Scoreboard, bossbar, заголовки, actionbar и задания.
- Книги и поддерживаемые интерфейсы модов; ниже показан Shine Studio.
- Перевод исходящих сообщений: включите **Translate Sent Chat** и нажмите **Ctrl+Enter**.

Исходный и целевой языки выбираются в настройках. По умолчанию — **автоопределение → русский**. Можно выбрать другой поддерживаемый язык или ввести его код вручную.

## Отличия от SimpleTranslate

| Что изменено | В этой версии |
|---|---|
| Переводчик | Google без ключа выбран по умолчанию; скачивать и запускать локальную модель не требуется. |
| Нейросети | API модели сохранён как отдельный режим. Переводит только выбранный провайдер; автоматического переключения между ним и Google нет. |
| Диалоги Wynncraft | Реплики переводятся в родной рамке, с сохранением заголовка NPC, управления и расположения видимых вариантов ответа. |
| Длинные реплики | Переносы строк и уменьшение текста. Если перевод не помещается даже при масштабе 60%, остаётся оригинал. |
| Запросы | Объединение текстов, устранение дублей, приоритет диалогов, ограничение частоты запросов Google и паузы при лимитах. |
| Хранение | Отдельная папка `config/simple_translate_web`; старые кеши SimpleTranslate и LM Studio автоматически не подмешиваются. |

Перевод чата, предметов, книг, табличек и интерфейсов, инструменты кеша и горячие клавиши основаны на оригинальном моде. Это самостоятельный форк, а не официальный релиз SimpleTranslate.

## Примеры в игре

Скриншоты сделаны в реальной игре автором форка. Изображения не изменены; нажмите на картинку, чтобы открыть полный размер. Машинный перевод может ошибаться и оставлять отдельные надписи без перевода.

### Предметы и их характеристики

Предмет Wynncraft: английский → русский / китайский. Цвета, иконки и числовые характеристики остаются на месте.

<table>
<tr><th>Английский — оригинал</th><th>Русский</th><th>Китайский</th></tr>
<tr><td valign="top"><a href="docs/images/item-wynn-en.png"><img src="docs/images/item-wynn-en.png" width="280" alt="Предметы и их характеристики: Английский — оригинал"></a></td><td valign="top"><a href="docs/images/item-wynn-ru.png"><img src="docs/images/item-wynn-ru.png" width="280" alt="Предметы и их характеристики: Русский"></a></td><td valign="top"><a href="docs/images/item-wynn-zh.png"><img src="docs/images/item-wynn-zh.png" width="280" alt="Предметы и их характеристики: Китайский"></a></td></tr>
</table>

### Диалоги Wynncraft внутри родной рамки

Перевод реплики появляется внутри коричневой рамки. Имя в заголовке и подсказка SHIFT остаются оригинальными.

<table>
<tr><th>Английский — оригинал</th><th>Русский</th><th>Китайский</th></tr>
<tr><td valign="top"><a href="docs/images/dialogue-wynn-en.png"><img src="docs/images/dialogue-wynn-en.png" width="280" alt="Диалоги Wynncraft внутри родной рамки: Английский — оригинал"></a></td><td valign="top"><a href="docs/images/dialogue-wynn-ru.png"><img src="docs/images/dialogue-wynn-ru.png" width="280" alt="Диалоги Wynncraft внутри родной рамки: Русский"></a></td><td valign="top"><a href="docs/images/dialogue-wynn-zh.png"><img src="docs/images/dialogue-wynn-zh.png" width="280" alt="Диалоги Wynncraft внутри родной рамки: Китайский"></a></td></tr>
</table>

### Имена NPC

Bandit → Бандит. Перевод имён сущностей можно отдельно выключить.

<table>
<tr><th>Английский — оригинал</th><th>Русский</th></tr>
<tr><td valign="top"><a href="docs/images/npc-wynn-en.png"><img src="docs/images/npc-wynn-en.png" width="430" alt="Имена NPC: Английский — оригинал"></a></td><td valign="top"><a href="docs/images/npc-wynn-ru.png"><img src="docs/images/npc-wynn-ru.png" width="430" alt="Имена NPC: Русский"></a></td></tr>
</table>

### Чат и сообщения NPC

Реплики Hypixel SkyBlock на английском, русском и китайском. Режим по кнопке позволяет переключаться между переводом и оригиналом.

<table>
<tr><th>Английский — оригинал</th><th>Русский</th><th>Китайский</th></tr>
<tr><td valign="top"><a href="docs/images/chat-hypixel-en.png"><img src="docs/images/chat-hypixel-en.png" width="280" alt="Чат и сообщения NPC: Английский — оригинал"></a></td><td valign="top"><a href="docs/images/chat-hypixel-ru.png"><img src="docs/images/chat-hypixel-ru.png" width="280" alt="Чат и сообщения NPC: Русский"></a></td><td valign="top"><a href="docs/images/chat-hypixel-zh.png"><img src="docs/images/chat-hypixel-zh.png" width="280" alt="Чат и сообщения NPC: Китайский"></a></td></tr>
</table>

### Интерфейсы модов: Shine Studio

Нажмите K в поддерживаемом интерфейсе. Пример также показывает ограничения: нестандартные заголовки могут переводиться частично, а длинный текст — выходить за границы виджета.

<table>
<tr><th>Английский — оригинал</th><th>Русский</th><th>Китайский</th></tr>
<tr><td valign="top"><a href="docs/images/shine-en.png"><img src="docs/images/shine-en.png" width="280" alt="Интерфейсы модов: Shine Studio: Английский — оригинал"></a></td><td valign="top"><a href="docs/images/shine-ru.png"><img src="docs/images/shine-ru.png" width="280" alt="Интерфейсы модов: Shine Studio: Русский"></a></td><td valign="top"><a href="docs/images/shine-zh.png"><img src="docs/images/shine-zh.png" width="280" alt="Интерфейсы модов: Shine Studio: Китайский"></a></td></tr>
</table>

<details>
<summary>Ещё примеры: Hypixel, HUD и текст в мире</summary>

#### Предмет Hypixel SkyBlock

<table>
<tr><th>Английский — оригинал</th><th>Русский</th></tr>
<tr><td valign="top"><a href="docs/images/item-hypixel-en.png"><img src="docs/images/item-hypixel-en.png" width="430" alt="Предмет Hypixel SkyBlock: Английский — оригинал"></a></td><td valign="top"><a href="docs/images/item-hypixel-ru.png"><img src="docs/images/item-hypixel-ru.png" width="430" alt="Предмет Hypixel SkyBlock: Русский"></a></td></tr>
</table>

#### Задания и scoreboard

<table>
<tr><th>Английский — оригинал</th><th>Русский</th></tr>
<tr><td valign="top"><a href="docs/images/hud-hypixel-en.png"><img src="docs/images/hud-hypixel-en.png" width="430" alt="Задания и scoreboard: Английский — оригинал"></a></td><td valign="top"><a href="docs/images/hud-hypixel-ru.png"><img src="docs/images/hud-hypixel-ru.png" width="430" alt="Задания и scoreboard: Русский"></a></td></tr>
</table>

#### Надписи в мире Wynncraft

<table>
<tr><th>Английский — оригинал</th><th>Русский</th></tr>
<tr><td valign="top"><a href="docs/images/world-wynn-en.png"><img src="docs/images/world-wynn-en.png" width="430" alt="Надписи в мире Wynncraft: Английский — оригинал"></a></td><td valign="top"><a href="docs/images/world-wynn-ru.png"><img src="docs/images/world-wynn-ru.png" width="430" alt="Надписи в мире Wynncraft: Русский"></a></td></tr>
</table>

#### Магазин Hypixel: три языка

<table>
<tr><th>Английский — оригинал</th><th>Русский</th><th>Китайский</th></tr>
<tr><td valign="top"><a href="docs/images/shop-hypixel-en.png"><img src="docs/images/shop-hypixel-en.png" width="280" alt="Магазин Hypixel: три языка: Английский — оригинал"></a></td><td valign="top"><a href="docs/images/shop-hypixel-ru.png"><img src="docs/images/shop-hypixel-ru.png" width="280" alt="Магазин Hypixel: три языка: Русский"></a></td><td valign="top"><a href="docs/images/shop-hypixel-zh.png"><img src="docs/images/shop-hypixel-zh.png" width="280" alt="Магазин Hypixel: три языка: Китайский"></a></td></tr>
</table>

</details>

## Установка и управление

1. Нужны **Minecraft Java Edition 26.2**, **Fabric Loader** и **Fabric API для 26.2**. Проверено с Loader **0.19.5**, Fabric API **0.155.2+26.2** и Java **25**.
2. Скачайте `.jar` из [Releases](https://github.com/mirray-u/Translate-Any-Server-and-Mod/releases/latest) и положите в папку `mods` своей сборки. Удалите другой JAR SimpleTranslate: идентификатор мода у них одинаковый.
3. После запуска Google уже выбран. Исходный язык определяется автоматически, целевой — русский. Языки можно изменить в настройках.
4. Настройки открываются на **U** по умолчанию или через необязательный **Mod Menu**. Для клиентского перевода установка на сервер не нужна.

| Действие | Как использовать |
|---|---|
| Перевести открытый интерфейс | **K**, если перевод интерфейсов включён. Повторное K не возвращает оригинал. |
| Временно показать оригинал | В **Shortcuts → Hold to Show Original** включить общий переключатель и назначить клавишу для **GUI text** или другой категории. Оригинал виден при удержании. |
| Перевести подсказку вручную | **V**, если выбран режим перевода по клавише. |
| Выбрать и перевести таблички | В режиме **Manual Selection**: **G** — включить/выключить выбор; **H** — перевести выбранное. |
| Перевести и отправить сообщение | Включить **Translate Sent Chat**, выбрать язык сервера, нажать **Ctrl+Enter**. |
| Исправить неудачный перевод | Отредактировать запись в **Translated Cache**. |

Готовые переводы сохраняются на диск и используются после перезапуска. Изменение текста, языка, провайдера или настроек кеша может потребовать нового перевода. Личного конфига и игрового кеша автора в скачиваемом моде нет.

## Совместимость и передача данных

Переводятся тексты серверов и модов, доступные поддерживаемым способам отрисовки. **Полный перевод любого элемента любого сервера или мода не гарантируется.** Нестандартные интерфейсы и шрифты иногда требуют адаптации. Текст на картинках и текстурах не распознаётся. В примерах Shine видны частичный перевод заголовков и выход длинного текста за границы виджетов.

Определение языка также имеет ограничения: текст латиницей может пропускаться при переводе на английский, а текст других языков с кириллицей — при переводе на русский. Примеры подтверждают английский → русский и английский → китайский, а не все возможные языковые пары.

**В режиме Google выбранные для перевода тексты отправляются Google; в режиме Model API — настроенному сервису модели.** Используются неофициальные веб-адреса Google: нужен интернет, возможны лимиты и сбои. При ошибке остаётся оригинал. Промпты и история контекста нейросети в Google не отправляются. Необязательный обмен кешем с совместимым сервером работает только при включении этой функции.

## Исходники и сборка

Полный изменённый код и тесты для Fabric 26.2 находятся в [`fabric/SimpleTranslate-Fabric-26.2`](fabric/SimpleTranslate-Fabric-26.2). Другие версии Minecraft в этот форк не входят.

С JDK 25 из этой папки выполните `gradlew.bat test build`; на Linux/macOS — `sh gradlew test build`. Параметр `-PliveGoogleTest` включает отдельные сетевые тесты Google. JAR создаётся в `build/libs`.

Опубликованная версия — **2.2.1-web.6**, совпадающая с проверенным JAR, установленным у автора. Контрольная сумма приложена к релизу.

## Авторы и лицензия

Оригинальный мод: **baokaixina — [SimpleTranslate](https://github.com/baokaixina/SimpleTranslate)**. Интеграция веб-перевода Google и доработка диалогов Wynncraft: **mirray-u**, с использованием помощи ИИ. Оригинальная лицензия MIT и уведомление об авторских правах сохранены в [LICENSE](LICENSE).

Wynncraft, Hypixel и Shine Studio показаны как примеры совместимости; их названия и графика принадлежат соответствующим авторам. Проект не связан с ними, Google, Mojang или Microsoft и не заявляет об их официальной поддержке.
