package community.leo.obd;

import android.bluetooth.*;
import android.os.*;
import android.content.Context;
import community.leo.obd.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Main looper owns all BLE and protocol state. No reconnection and no arbitrary writes. */
@SuppressWarnings("deprecation")
final class BleConnection {
    interface Listener {
        void state(String text,boolean live);
        void sample(int pid,Double value);
        void diagnostic(String mode,List<String> codes);
    }
    private static final UUID SERVICE=UUID.fromString("000018f0-0000-1000-8000-00805f9b34fb");
    private static final UUID TX=UUID.fromString("00002af1-0000-1000-8000-00805f9b34fb");
    private static final UUID RX=UUID.fromString("00002af0-0000-1000-8000-00805f9b34fb");
    private static final UUID CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final List<String> INIT=Arrays.asList("ATZ","ATE0","ATL0","ATS0","ATH0","ATSP0","0100");
    private final Context context;private final Listener listener;private final Handler main=new Handler(Looper.getMainLooper());
    private BluetoothGatt gatt;private BluetoothGattCharacteristic tx;
    private RequestQueue queue;private long epoch,setupDeadline,nextPoll,writeDeadline;private int stage,index,pollIndex;
    private boolean ready,writing,dtcPending;private final Set<Integer> supported=new HashSet<>();
    BleConnection(Context context,Listener listener){this.context=context;this.listener=listener;}
    void connect(BluetoothDevice device){
        stop("Подключение…");long owner=epoch;queue=new RequestQueue();setupDeadline=now()+25000;
        try{gatt=device.connectGatt(context,false,callback(owner),BluetoothDevice.TRANSPORT_LE);
            if(gatt==null){stop("Не удалось открыть BLE");return;}main.post(tick);
        }catch(SecurityException e){stop("Разреши доступ к Bluetooth в настройках Android");}
    }
    void stop(String reason){
        epoch++;main.removeCallbacks(tick);if(queue!=null)queue.cancel();queue=null;ready=false;writing=false;dtcPending=false;
        stage=0;index=0;pollIndex=0;nextPoll=0;supported.clear();tx=null;
        BluetoothGatt old=gatt;gatt=null;if(old!=null){try{old.disconnect();}catch(SecurityException ignored){}try{old.close();}catch(SecurityException ignored){}}
        listener.state(reason,false);
    }
    private long now(){return SystemClock.elapsedRealtime();}
    private void event(long owner,BluetoothGatt source,Runnable action){main.post(()->{if(owner==epoch&&source==gatt){try{action.run();}catch(RuntimeException failure){stop("Ошибка Bluetooth. Подключись заново.");}}});}
    private BluetoothGattCallback callback(long owner){return new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int status,int state){event(owner,g,()->{
            if(status!=BluetoothGatt.GATT_SUCCESS||state==BluetoothProfile.STATE_DISCONNECTED){stop("Связь с адаптером завершена");return;}
            if(state==BluetoothProfile.STATE_CONNECTED&&stage==0){stage=1;listener.state("Проверяю профиль адаптера…",false);try{if(!g.discoverServices())stop("Не удалось прочитать BLE-профиль");}catch(SecurityException denied){stop("Доступ к Bluetooth отозван");}}
        });}
        @Override public void onServicesDiscovered(BluetoothGatt g,int status){event(owner,g,()->{
            try {
            if(stage!=1||status!=BluetoothGatt.GATT_SUCCESS){stop("BLE-профиль недоступен");return;}
            BluetoothGattService service=g.getService(SERVICE);tx=service==null?null:service.getCharacteristic(TX);
            BluetoothGattCharacteristic rx=service==null?null:service.getCharacteristic(RX);
            BluetoothGattDescriptor descriptor=rx==null?null:rx.getDescriptor(CCCD);
            if(tx==null||rx==null||descriptor==null||(tx.getProperties()&BluetoothGattCharacteristic.PROPERTY_WRITE)==0
                ||(rx.getProperties()&BluetoothGattCharacteristic.PROPERTY_NOTIFY)==0){stop("Этот BLE-профиль пока не поддерживается");return;}
            if(!g.setCharacteristicNotification(rx,true)){stop("Не удалось включить приём ответов");return;}stage=2;
            boolean accepted;
            if(Build.VERSION.SDK_INT>=33)accepted=g.writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)==BluetoothStatusCodes.SUCCESS;
            else {descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);accepted=g.writeDescriptor(descriptor);}
            if(!accepted)stop("Не удалось настроить приём ответов");
            }catch(SecurityException denied){stop("Доступ к Bluetooth отозван");}
        });}
        @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){event(owner,g,()->{
            if(stage!=2||!CCCD.equals(d.getUuid())||status!=BluetoothGatt.GATT_SUCCESS){stop("Адаптер не подтвердил приём ответов");return;}
            stage=3;listener.state("Проверяю стандартные OBD-показатели…",false);queue.enqueue(INIT.get(0));
        });}
        @Override public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int status){event(owner,g,()->{
            if(!TX.equals(c.getUuid())||!writing||status!=BluetoothGatt.GATT_SUCCESS){stop("Команда не передана. Подключись заново.");return;}writing=false;
        });}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value){byte[] copy=value.clone();event(owner,g,()->receive(c,copy));}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){byte[] value=c.getValue();byte[] copy=value==null?new byte[0]:value.clone();event(owner,g,()->receive(c,copy));}
    };}
    private void receive(BluetoothGattCharacteristic characteristic,byte[] bytes){
        if(!RX.equals(characteristic.getUuid())||queue==null)return;
        RequestQueue.Reply result=queue.receive(new String(bytes,StandardCharsets.US_ASCII),now());
        if(queue.failed()){stop("Ответ потерян или повреждён. Подключись заново.");return;}
        if(result!=null)handle(result);
    }
    private void handle(RequestQueue.Reply reply){
        String command=reply.command();
        try{
            if(command.startsWith("AT")){
                List<String> lines=ObdCodec.lines(reply.body(),command);
                boolean ok=command.equals("ATZ")?lines.stream().anyMatch(x->x.contains("ELM")||x.contains("VLINKER")||x.contains("MIC")||x.contains("STN")):lines.equals(Arrays.asList("OK"));
                if(!ok)throw new IllegalArgumentException("Unrecognized adapter");queue.enqueue(INIT.get(++index));return;
            }
            if(Arrays.asList("0100","0120","0140","0160").contains(command)){
                int base=Integer.parseInt(command.substring(2),16);Set<Integer> map=ObdCodec.supported(reply.body(),base);supported.addAll(map);
                if(base<64&&map.contains(base+32)){queue.enqueue(ObdCodec.command(base+32));return;}
                ready=true;listener.state("Подключено · только чтение двигателя",true);
                for(int pid:ObdCodec.PIDS)if(!supported.contains(pid))listener.sample(pid,null);
                if(ObdCodec.PIDS.stream().noneMatch(supported::contains))listener.state("Связь есть; выбранные показатели не поддерживаются",true);
                return;
            }
            if(command.equals("03")||command.equals("07")){
                listener.diagnostic(command,ObdCodec.dtc(reply.body(),command));if(command.equals("07"))dtcPending=false;return;
            }
            int pid=Integer.parseInt(command.substring(2),16);listener.sample(pid,ObdCodec.value(reply.body(),pid));
        }catch(IllegalArgumentException bad){
            if(!ready){stop("Не удалось определить совместимость OBD. Чтение остановлено.");return;}
            if(command.equals("03")||command.equals("07")){listener.diagnostic(command,null);if(command.equals("07"))dtcPending=false;}
            else {int pid=Integer.parseInt(command.substring(2),16);supported.remove(pid);listener.sample(pid,null);}
        }
    }
    void readDiagnostics(){if(!ready||dtcPending||queue==null)return;dtcPending=true;queue.enqueue("03");queue.enqueue("07");}
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(gatt==null||queue==null)return;
        long time=now();
        if(!ready&&time>=setupDeadline||queue.expire(time)||writing&&time>=writeDeadline){stop("Время ожидания истекло. Автоповтора нет.");return;}
        if(ready&&queue.idle()&&!writing&&time>=nextPoll){
            nextPoll=time+1000;
            for(int n=0;n<ObdCodec.PIDS.size();n++){
                int pid=ObdCodec.PIDS.get(pollIndex++%ObdCodec.PIDS.size());if(supported.contains(pid)){queue.enqueue(ObdCodec.command(pid));break;}
            }
        }
        if(stage==3&&!writing){String command=queue.next(time);if(command!=null&&!write(command))return;}
        main.postDelayed(this,100);
    }};
    private boolean write(String command){
        byte[] frame=ReadPolicy.frame(command); // Final boundary; not caller-supplied bytes.
        writing=true;writeDeadline=now()+10000;
        try{
            boolean accepted;
            if(Build.VERSION.SDK_INT>=33)accepted=gatt.writeCharacteristic(tx,frame,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)==BluetoothStatusCodes.SUCCESS;
            else {tx.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);tx.setValue(frame);accepted=gatt.writeCharacteristic(tx);}
            if(!accepted){stop("Адаптер не принял команду");return false;}
            return true;
        }catch(SecurityException permission){stop("Нет разрешения Bluetooth");return false;}
    }
}
