package app.game_logic;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class Timer {
    private Timeline timeline;
    private int secondsRemaining;
    private Runnable onTick;
    private Runnable onFinish;

    public Timer(int totalSeconds, Runnable onTick, Runnable onFinish){
        this.secondsRemaining = totalSeconds;
        this.onTick = onTick;
        this.onFinish = onFinish;
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void tick(){
        secondsRemaining--;
        if(onTick != null) onTick.run();
        if(secondsRemaining <= 0){
            timeline.stop();
            if(onFinish != null) onFinish.run();
        }
    }  


    public void start(){
        timeline.play();
    }

    public void stop(){
        timeline.stop();
    }

    public int getSecondsRemaining(){
        return secondsRemaining;
    }

    public String getFormattedTime(){
        int minutes = secondsRemaining/60, 
        seconds = secondsRemaining % 60;
        return String.format("%02d:%02d", minutes, seconds  );
    }

}


