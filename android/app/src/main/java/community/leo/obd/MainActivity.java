package community.leo.obd;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import community.leo.obd.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Local-only UI. No cloud account, provider key or private vehicle identity. */
@SuppressWarnings("deprecation")
public final class MainActivity extends Activity implements BleConnection.Listener {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService disk=Executors.newSingleThreadExecutor();
    private final Map<Integer,TextView> fields=new LinkedHashMap<>();
    private final Map<Integer,Long> measured=new HashMap<>();
    private final List<BluetoothDevice> found=new ArrayList<>();
    private final List<String> foundLabels=new ArrayList<>();
    private LinearLayout layout;private TextView status,badge,diagnostics,logStatus;
    private Button read,export,stop;private BleConnection ble;
    private BluetoothLeScanner scanner;private ScanCallback scanning;private AlertDialog scanDialog;
    private SessionLog log;private LocalLogStore localStore;private LatestSnapshotWriter logWriter;private long appended;private String recoveredCsv;
    private boolean demo,live,visible;private long ticks,scanEpoch,saveEpoch;private String exportSnapshot;
    private static final int INK=Color.rgb(229,239,237),MUTED=Color.rgb(154,177,173),GREEN=Color.rgb(110,231,200);
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);getWindow().setStatusBarColor(Color.rgb(10,19,22));getWindow().setNavigationBarColor(Color.rgb(10,19,22));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(10,19,22));
        layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(22),dp(24),dp(22),dp(24));scroll.addView(layout);setContentView(scroll);
        if(Build.VERSION.SDK_INT>=35)scroll.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());layout.setPadding(dp(22),dp(24)+bars.top,dp(22),dp(24)+bars.bottom);return insets;
        });
        text("LEO / COMMUNITY",14,GREEN);TextView title=text("Твоя машина.\nТвои данные.",32,INK);title.setTypeface(null,Typeface.BOLD);
        text("Локальный OBD и демонстрация · 0.1 preview",14,MUTED);
        badge=text("БЕЗ ОБЛАКА · БЕЗ ПЛАТНЫХ ВЫЗОВОВ",12,GREEN);
        status=text("Начни с demo — адаптер и регистрация не нужны.",16,INK);
        button("Попробовать без машины",v->{
            if((log!=null&&log.size()>0)||recoveredCsv!=null)new AlertDialog.Builder(this).setTitle("Новая demo-сессия")
                .setMessage("Предыдущий журнал будет заменён. Если он нужен, сначала сохрани CSV.")
                .setNegativeButton("Отмена",null).setPositiveButton("Начать demo",(d,w)->startDemo()).show();
            else startDemo();
        });
        button("Подключить BLE-адаптер",v->requestScan());
        stop=button("Остановить",v->stopAll("Остановлено. Журнал можно сохранить."));
        for(int pid:ObdCodec.PIDS){TextView view=text(label(pid)+"\n—",22,INK);view.setPadding(dp(16),dp(12),dp(16),dp(12));
            GradientDrawable card=new GradientDrawable();card.setColor(Color.rgb(20,34,38));card.setCornerRadius(dp(14));view.setBackground(card);fields.put(pid,view);}
        diagnostics=text("Ошибки двигателя ещё не прочитаны. SRS, ABS и коробка здесь не проверяются.",14,MUTED);
        read=button("Прочитать ошибки двигателя",v->{
            if(demo){diagnostics.setText("DEMO: сохранённый P0171; ожидающих кодов нет. Это вымышленный пример.");append("stored_dtc","P0171","","OK");}
            else if(live){diagnostics.setText("Читаю сохранённые и ожидающие коды двигателя…");storedResult="";pendingResult="";ble.readDiagnostics();}
        });read.setEnabled(false);
        logStatus=text("Журнал пока пуст",13,MUTED);
        export=button("Сохранить CSV…",v->saveDocument());export.setEnabled(false);
        button("Как подключить и что доступно",v->new AlertDialog.Builder(this).setTitle("Быстрый старт")
            .setMessage("1. Для demo ничего подключать не нужно. Все числа вымышленные.\n\n2. Для настоящих показаний нужен совместимый BLE-адаптер. Первая версия рассчитана на профиль vLinker FD+. Bluetooth Classic, Wi-Fi и другие профили пока не поддерживаются.\n\n3. Припаркуй машину, подключи адаптер в OBD-разъём и включи зажигание. Разреши Bluetooth и выбери свой адаптер.\n\n4. Приложение читает только поддерживаемые показатели. Топливо — проценты, не литры; одометр и SRS не читаются.\n\n5. Сохрани CSV до закрытия приложения. При сворачивании связь и запись останавливаются.\n\nИнтернет, AI-ключ и Telegram для этой версии не нужны. Никакие данные автоматически не отправляются.")
            .setPositiveButton("Понятно",null).show());
        text("Preview: новая Community-сборка ещё требует проверки на машине. Подключение и настройки выполняй на стоянке.",12,MUTED);
        ble=new BleConnection(this,this);localStore=new LocalLogStore(new File(getFilesDir(),"local-log"));
        logWriter=new LatestSnapshotWriter(disk,localStore::save,failed->main.post(()->logStatus.setText("Не удалось сохранить журнал. Экспортируй CSV сейчас.")));
        long loadingEpoch=saveEpoch;disk.execute(()->{try{String previous=localStore.read();main.post(()->{
            if(log==null&&loadingEpoch==saveEpoch&&previous!=null){recoveredCsv=previous;export.setEnabled(true);logStatus.setText("Сохранён предыдущий журнал. Можно экспортировать CSV.");}
        });}catch(IOException unavailable){main.post(()->logStatus.setText("Предыдущий журнал недоступен"));}});
    }
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);layout.addView(t,p);return t;}
    private Button button(String label,View.OnClickListener action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(INK);
        b.setOnClickListener(action);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);layout.addView(b,p);return b;}
    private static String label(int pid){return switch(pid){case 12->"Обороты";case 13->"Скорость";case 5->"Температура ОЖ";case 4->"Нагрузка двигателя";case 17->"Дроссель";case 47->"Уровень топлива";case 66->"Напряжение ЭБУ";default->"Показатель";};}
    private static String unit(int pid){return switch(pid){case 12->"rpm";case 13->"km/h";case 5->"C";case 66->"V";default->"%";};}
    private static String field(int pid){return switch(pid){case 12->"rpm";case 13->"speed";case 5->"coolant";case 4->"load";case 17->"throttle";case 47->"fuel_percent";default->"module_voltage";};}
    private void newSession(String source){saveEpoch++;appended=0;recoveredCsv=null;log=new SessionLog(source,10000);persist();measured.clear();for(int pid:ObdCodec.PIDS)fields.get(pid).setText(label(pid)+"\n—");
        diagnostics.setText(source.equals("DEMO")?"DEMO: диагностические примеры вымышленные.":"Ошибки двигателя ещё не прочитаны.");export.setEnabled(false);logStatus.setText("Новая локальная сессия · "+source);}
    private void startDemo(){
        stopAll("Демонстрация запущена");newSession("DEMO");demo=true;ticks=0;read.setEnabled(true);
        badge.setText("DEMO · ВСЕ ПОКАЗАНИЯ ВЫМЫШЛЕННЫЕ");status.setText("Работает без машины и интернета");main.post(demoTick);
    }
    private final Runnable demoTick=new Runnable(){public void run(){if(!visible||!demo)return;for(int pid:ObdCodec.PIDS)sample(pid,ObdCodec.value(DemoVehicle.response(pid,ticks),pid));ticks++;main.postDelayed(this,2000);}};
    private final Runnable ageTick=new Runnable(){public void run(){if(!visible)return;if(live){long now=SystemClock.elapsedRealtime();for(var entry:measured.entrySet())if(now-entry.getValue()>15000)fields.get(entry.getKey()).setText(label(entry.getKey())+"\nОжидаю новый замер");}main.postDelayed(this,1000);}};
    private void append(String field,String value,String unit,String outcome){if(log==null)return;log.append(System.currentTimeMillis(),field,value,unit,outcome);export.setEnabled(true);logStatus.setText("В журнале: "+log.size()+" строк · максимум 10 000");if(++appended%10==0)persist();}
    private void persist(){if(logWriter!=null&&log!=null)logWriter.offer(log.csv());}
    @Override public void sample(int pid,Double value){
        String number=value==null?"":String.format(Locale.ROOT,pid==66?"%.2f":"%.0f",value);
        fields.get(pid).setText(label(pid)+"\n"+(value==null?"Недоступно":number+" "+unit(pid)));
        if(value!=null)measured.put(pid,SystemClock.elapsedRealtime());else measured.remove(pid);
        append(field(pid),number,unit(pid),value==null?"UNAVAILABLE":"OK");
    }
    private String storedResult="",pendingResult="";
    @Override public void diagnostic(String mode,List<String> codes){String value=codes==null?"прочитать не удалось":codes.isEmpty()?"не обнаружены":String.join(", ",codes);
        if(mode.equals("03"))storedResult="Сохранённые: "+value;else pendingResult="Ожидающие: "+value;
        diagnostics.setText(storedResult+"\n"+pendingResult+"\nТолько стандартная диагностика двигателя.");
        append(mode.equals("03")?"stored_dtc":"pending_dtc",codes==null?"":String.join(";",codes),"",codes==null?"UNAVAILABLE":"OK");
    }
    @Override public void state(String text,boolean connected){live=connected;status.setText(text);read.setEnabled(connected||demo);
        if(!connected&&!demo){measured.clear();for(int pid:ObdCodec.PIDS)fields.get(pid).setText(label(pid)+"\n—");}}
    private void stopAll(String reason){demo=false;main.removeCallbacks(demoTick);stopScan();if(ble!=null)ble.stop(reason);badge.setText("ЛОКАЛЬНО · СВЯЗЬ ОСТАНОВЛЕНА");read.setEnabled(false);}
    private void requestScan(){
        if(Build.VERSION.SDK_INT>=31){if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},10);return;}}
        else if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},10);return;}
        startScan();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);
        if(code==10&&results.length>0){for(int result:results)if(result!=PackageManager.PERMISSION_GRANTED){status.setText("Bluetooth не разрешён. Demo работает без разрешений.");return;}startScan();}}
    private void startScan(){
        stopAll("Ищу BLE-адаптеры…");BluetoothManager manager=getSystemService(BluetoothManager.class);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
        if(adapter==null){status.setText("На этом устройстве нет Bluetooth. Доступен demo.");return;}
        try{if(!adapter.isEnabled()){status.setText("Включи Bluetooth в настройках телефона");return;}scanner=adapter.getBluetoothLeScanner();if(scanner==null){status.setText("BLE-сканер недоступен");return;}
            found.clear();foundLabels.clear();ArrayAdapter<String> list=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,foundLabels);
            scanDialog=new AlertDialog.Builder(this).setTitle("Выбери свой BLE-адаптер").setAdapter(list,(d,index)->{
                BluetoothDevice selected=found.get(index);stopScan();new AlertDialog.Builder(this).setTitle("Подключиться на стоянке?")
                    .setMessage("Зажигание включено, машина припаркована. Выполняются только стандартные чтения. Новая сессия заменит журнал на экране; сохрани прежний CSV заранее.")
                    .setNegativeButton("Отмена",null).setPositiveButton("Стою, подключить",(dialog,which)->{newSession("OBD");badge.setText("OBD · ТОЛЬКО ЧТЕНИЕ");ble.connect(selected);}).show();
            }).setNegativeButton("Закрыть",(d,w)->stopScan()).create();scanDialog.setOnDismissListener(d->stopScan());scanDialog.show();
            long owner=++scanEpoch;scanning=new ScanCallback(){@Override public void onScanResult(int type,ScanResult result){main.post(()->{
                if(owner!=scanEpoch||!visible||scanning==null)return;BluetoothDevice device=result.getDevice();if(found.contains(device)||found.size()>=30)return;
                try{String name=device.getName();found.add(device);foundLabels.add((name==null?"BLE-устройство":name)+" · "+device.getAddress());list.notifyDataSetChanged();}catch(SecurityException ignored){}
            });}@Override public void onScanFailed(int code){main.post(()->{if(owner==scanEpoch){stopScan();status.setText("Сканирование недоступно. Подожди и попробуй снова.");}});}};
            scanner.startScan(scanning);main.postDelayed(()->{if(owner==scanEpoch){stopScan();status.setText(found.isEmpty()?"Адаптер не найден. Проверь питание; на Android 8–11 включи геолокацию.":"Поиск завершён. Выбери адаптер из списка.");}},10000);
        }catch(SecurityException permission){status.setText("Разреши Bluetooth в настройках Android");stopScan();}
    }
    private void stopScan(){scanEpoch++;if(scanner!=null&&scanning!=null)try{scanner.stopScan(scanning);}catch(SecurityException ignored){}scanning=null;scanner=null;}
    private void saveDocument(){if((log==null||log.size()==0)&&recoveredCsv==null)return;exportSnapshot=log==null?recoveredCsv:log.csv();Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/csv").addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE,"leo-"+(demo?"demo":"session")+"-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new Date())+".csv");try{startActivityForResult(intent,20);}catch(ActivityNotFoundException missing){exportSnapshot=null;status.setText("На устройстве нет системного выбора файлов");}}
    @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);String snapshot=exportSnapshot;exportSnapshot=null;
        if(request==20&&result==RESULT_OK&&data!=null&&data.getData()!=null&&snapshot!=null){android.net.Uri uri=data.getData();disk.execute(()->{
            try(OutputStream stream=getContentResolver().openOutputStream(uri,"wt")){if(stream==null)throw new IOException();stream.write(snapshot.getBytes(StandardCharsets.UTF_8));main.post(()->status.setText("CSV сохранён в выбранное место"));}
            catch(Exception error){main.post(()->status.setText("Не удалось сохранить CSV. Повтори экспорт."));}
        });}}
    @Override public void onStart(){super.onStart();visible=true;main.post(ageTick);}
    @Override public void onStop(){visible=false;main.removeCallbacks(ageTick);stopAll("Связь остановлена при сворачивании. Подключись заново.");persist();if(scanDialog!=null)scanDialog.dismiss();super.onStop();}
    @Override public void onDestroy(){disk.shutdown();super.onDestroy();}
}
