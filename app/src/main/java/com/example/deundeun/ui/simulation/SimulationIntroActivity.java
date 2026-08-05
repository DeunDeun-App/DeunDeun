package com.example.deundeun.ui.simulation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivitySimulationIntroBinding;
import com.example.deundeun.databinding.ViewNumberedStepBinding;
import com.example.deundeun.databinding.ViewScenarioCardBinding;
import com.example.deundeun.ui.common.InsetsUtil;

/** 재난 시뮬레이션 시나리오 선택 */
public class SimulationIntroActivity extends AppCompatActivity {

    public static final String EXTRA_SCENARIO = "scenario";

    private static final String STATE_FIRE_SELECTED = "fire_selected";

    private ActivitySimulationIntroBinding binding;
    private boolean fireSelected;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySimulationIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.simIntroScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        binding.header.setOnBackClickListener(v -> finish());

        if (savedInstanceState != null) {
            fireSelected = savedInstanceState.getBoolean(STATE_FIRE_SELECTED, false);
        }

        bindScenario(binding.scenarioEarthquake,
                R.string.sim_scenario_earthquake, R.string.sim_scenario_earthquake_desc);
        bindScenario(binding.scenarioFire,
                R.string.sim_scenario_fire, R.string.sim_scenario_fire_desc);

        binding.scenarioEarthquake.getRoot().setOnClickListener(v -> selectScenario(false));
        binding.scenarioFire.getRoot().setOnClickListener(v -> selectScenario(true));

        bindStep(binding.howto1, 1, R.string.sim_howto_1);
        bindStep(binding.howto2, 2, R.string.sim_howto_2);
        bindStep(binding.howto3, 3, R.string.sim_howto_3);

        binding.btnStart.setOnClickListener(v -> startSimulation());

        applySelection();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_FIRE_SELECTED, fireSelected);
    }

    private void bindScenario(ViewScenarioCardBinding card,
                              @StringRes int titleRes, @StringRes int descRes) {
        card.scenarioTitle.setText(titleRes);
        card.scenarioDesc.setText(descRes);
    }

    private void bindStep(ViewNumberedStepBinding step, int number, @StringRes int labelRes) {
        step.stepNumber.setText(String.valueOf(number));
        step.stepLabel.setText(labelRes);
        step.getRoot().setContentDescription(number + ". " + getString(labelRes));
    }

    private void selectScenario(boolean fire) {
        fireSelected = fire;
        applySelection();
    }

    private void applySelection() {
        styleScenario(binding.scenarioEarthquake, !fireSelected,
                R.string.sim_scenario_earthquake, R.string.sim_scenario_earthquake_desc);
        styleScenario(binding.scenarioFire, fireSelected,
                R.string.sim_scenario_fire, R.string.sim_scenario_fire_desc);
    }

    private void styleScenario(ViewScenarioCardBinding card, boolean selected,
                               @StringRes int titleRes, @StringRes int descRes) {
        card.scenarioBox.setBackgroundResource(
                selected ? R.drawable.bg_scenario_selected : 0);
        card.scenarioCheck.setVisibility(selected ? View.VISIBLE : View.GONE);

        card.getRoot().setContentDescription(getString(titleRes) + ", " + getString(descRes)
                + (selected ? ", " + getString(R.string.cd_selected) : ""));
    }

    private void startSimulation() {
        String scenario = getString(fireSelected
                ? R.string.sim_scenario_fire : R.string.sim_scenario_earthquake);

        Intent intent = new Intent(this, SimulationRunActivity.class);
        intent.putExtra(EXTRA_SCENARIO, scenario);
        startActivity(intent);
    }
}
