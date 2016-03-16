package cnsl.augear;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

/**
 * Created by leeyg on 2016-03-06.
 */
public class CountdownFragment extends Fragment {
    private TextView numberView;
    private int maxLatencyInSec;
    private CountDownTimer timer;

    /**
     * We assume wifi latency must be less than maxLatencyInSec.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.countdownfrag, container, false);
        numberView = (TextView) view.findViewById(R.id.num_countdown);
        maxLatencyInSec = SharedConstants.CALIBRATION_LENGTH_IN_MS /1000;

        timer = new CountDownTimer((maxLatencyInSec+1) * 1000 + 100, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                numberView.setText("" + maxLatencyInSec--);
            }

            @Override
            public void onFinish() {
                numberView.setText("Processing...");
                Animation anim1 = new AlphaAnimation(0.0f, 1.0f);
                anim1.setDuration(150);
                anim1.setRepeatCount(Animation.INFINITE);
                anim1.setStartOffset(0);
                anim1.setRepeatMode(Animation.REVERSE);
                numberView.startAnimation(anim1);
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        timer.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }
}
