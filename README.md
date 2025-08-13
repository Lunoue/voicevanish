# VoiceVanishSuite (Paper 1.21.6)

Плагин для Paper:
- скрывает игроков в ванише (Essentials) из `/groups invite|list|info` (Plasmo/Plasma Voice Groups);
- опционально (через ProtocolLib) глушит сообщения о входе/выходе в voice‑группы, если их источник — игрок в ванише.

## Сборка
```
mvn -q -DskipTests package
```
Готовый файл: `target/VoiceVanishSuite-1.0.0.jar`

## Установка
1) Положите `.jar` в `plugins/` и перезапустите сервер.  
2) (Опционально) Установите ProtocolLib и включите в `plugins/VoiceVanishSuite/config.yml`:
```
protocolLib:
  enabled: true
```
3) Команды для перехвата настраиваются в `config.yml`.
