# 🎬 Ultimate Player v1.0.0

**100 funkcji w jednym APK!** Android TV / Phone / Box IPTV Player z LibVLC.

## 🚀 Funkcje (5 Filarów):
- **JĄDRO**: HW Acceleration, WakeLock, TCP, Deinterlace, 4K HDR
- **UI/UX**: TV Focus + D-Pad, Smooth Animations, Yellow Theme
- **M3U**: Parser EXTINF, Async Scan, Channel Cleaning
- **GUARDIANS**: Freeze Detection, Smart Reconnect, Network Monitor
- **STEROWANIE**: Sliders (Brightness/Contrast), CH+/-, Gestures

## 📱 Użycie:
1. Wklej link M3U playlisty
2. Skanuj → automatycznie odtworzy pierwszy kanał
3. DPAD / CH+/- / Menu / Gestures do nawigacji

## 🔧 Build APK:
```bash
./gradlew assembleRelease

```bash
# Backup rules (Git Freeze #2)
cat > app/src/main/res/xml/backup_rules.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- Git Freeze: No backup dla ochrony kodu -->
    <exclude domain="file" path="."/>
    <exclude domain="sharedpref" path="ultimate_player.xml"/>
</full-backup-content>
# Player_Max_Ai
