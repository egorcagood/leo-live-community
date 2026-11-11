from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
class Boundary(unittest.TestCase):
    def test_api_26_without_core_library_desugaring(self):
        for path in (ROOT/'android/app/src/main/java').rglob('*.java'):
            text=path.read_text()
            self.assertNotIn('List.of(',text,str(path))
            self.assertNotIn('Set.of(',text,str(path))
    def test_manifest_has_no_cloud_or_diagnostics_entry(self):
        root=ET.parse(ROOT/'android/app/src/main/AndroidManifest.xml').getroot()
        ns='{http://schemas.android.com/apk/res/android}'
        permissions={p.get(ns+'name') for p in root.findall('uses-permission')}
        self.assertNotIn('android.permission.INTERNET',permissions)
        self.assertNotIn('android.permission.RECORD_AUDIO',permissions)
        self.assertFalse(root.findall('.//provider'))
        self.assertEqual(root.find('application').get(ns+'allowBackup'),'false')
        self.assertEqual(root.find('uses-feature').get(ns+'required'),'false')
    def test_no_private_app_id_or_embedded_publisher(self):
        text=(ROOT/'android/app/build.gradle.kts').read_text()
        self.assertIn('community.leo.obd',text)
        for forbidden in ['LEO_LIVE_CONTEXT_PUBLISH_TOKEN','LEO_TRIP_EVENTS_TOKEN','live.leo.obd"']:
            self.assertNotIn(forbidden,text)
    def test_wire_write_requires_read_policy(self):
        text=(ROOT/'android/app/src/main/java/community/leo/obd/BleConnection.java').read_text()
        self.assertIn('ReadPolicy.frame(command)',text)
        self.assertEqual(text.count('writeCharacteristic('),2)
        self.assertNotIn('getIntent()',text)
if __name__=='__main__':unittest.main()
