package com.example.deundeun.ui.training;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.example.deundeun.R;
import com.example.deundeun.databinding.FragmentTrainingBinding;
import com.example.deundeun.databinding.ViewTrainingMenuCardBinding;
import com.example.deundeun.ui.call.CallIntroActivity;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.simulation.SimulationIntroActivity;
import com.example.deundeun.ui.translate.DisasterTranslateActivity;

/** 훈련탭 */
public class TrainingFragment extends Fragment {

    public static final String TAG = "training";

    private FragmentTrainingBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTrainingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        InsetsUtil.applyBottomInset(binding.trainingScroll);

        bindMenuCard(binding.cardSimulation,
                R.string.training_sim_eyebrow,
                R.string.training_sim_title,
                R.string.training_sim_desc);

        bindMenuCard(binding.cardEmergencyCall,
                R.string.training_call_eyebrow,
                R.string.training_call_title,
                R.string.training_call_desc);

        bindMenuCard(binding.cardTranslate,
                R.string.training_translate_eyebrow,
                R.string.training_translate_title,
                R.string.training_translate_desc);

        binding.cardSimulation.getRoot().setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SimulationIntroActivity.class)));
        binding.cardEmergencyCall.getRoot().setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CallIntroActivity.class)));
        binding.cardTranslate.getRoot().setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DisasterTranslateActivity.class)));
    }

    private void bindMenuCard(ViewTrainingMenuCardBinding card,
                              @StringRes int eyebrow,
                              @StringRes int title,
                              @StringRes int description) {
        card.menuEyebrow.setText(eyebrow);
        card.menuTitle.setText(title);
        card.menuDesc.setText(description);

        card.getRoot().setContentDescription(
                getString(title) + ". " + getString(eyebrow) + ". " + getString(description));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
