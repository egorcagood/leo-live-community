package community.leo.obd.core;
import java.util.Locale;

/** Synthetic measurements, not captured from any owner or vehicle. */
public final class DemoVehicle {
    private DemoVehicle(){}
    public static String response(int pid,long tick){
        int cycle=(int)Math.floorMod(tick,60);int raw=switch(pid){
            case 12->(cycle<10?760:1500+cycle*12)*4;
            case 13->cycle<10?0:Math.min(50,cycle);
            case 5->120+Math.min(10,cycle/6);
            case 4->42;case 17->28;case 47->128;case 66->13800;
            default->throw new IllegalArgumentException("Unsupported demo PID");};
        return String.format(Locale.ROOT,pid==12||pid==66?"41%02X%04X":"41%02X%02X",pid,raw);
    }
}
