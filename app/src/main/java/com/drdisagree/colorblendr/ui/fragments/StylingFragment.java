package com.drdisagree.colorblendr.ui.fragments;

import static com.drdisagree.colorblendr.common.Const.MANUAL_OVERRIDE_COLORS;
import static com.drdisagree.colorblendr.common.Const.MONET_ACCENT_SATURATION;
import static com.drdisagree.colorblendr.common.Const.MONET_ACCURATE_SHADES;
import static com.drdisagree.colorblendr.common.Const.MONET_BACKGROUND_LIGHTNESS;
import static com.drdisagree.colorblendr.common.Const.MONET_BACKGROUND_SATURATION;
import static com.drdisagree.colorblendr.common.Const.MONET_LAST_UPDATED;
import static com.drdisagree.colorblendr.common.Const.MONET_PITCH_BLACK_THEME;
import static com.drdisagree.colorblendr.common.Const.MONET_SEED_COLOR;
import static com.drdisagree.colorblendr.common.Const.MONET_SEED_COLOR_ENABLED;
import static com.drdisagree.colorblendr.common.Const.MONET_STYLE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.drdisagree.colorblendr.R;
import com.drdisagree.colorblendr.config.RPrefs;
import com.drdisagree.colorblendr.databinding.FragmentStylingBinding;
import com.drdisagree.colorblendr.ui.viewmodel.SharedViewModel;
import com.drdisagree.colorblendr.utils.ColorUtil;
import com.drdisagree.colorblendr.utils.MiscUtil;
import com.drdisagree.colorblendr.utils.OverlayManager;
import com.drdisagree.colorblendr.utils.WallpaperUtil;
import com.drdisagree.colorblendr.utils.monet.ColorSchemeUtil;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog;
import me.jfenn.colorpickerdialog.views.picker.ImagePickerView;

public class StylingFragment extends Fragment {

    private static final String TAG = StylingFragment.class.getSimpleName();
    private FragmentStylingBinding binding;
    private SharedViewModel sharedViewModel;
    private LinearLayout[] colorTableRows;
    private static final int[] colorCodes = {
            0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000
    };
    private final int[] monetAccentSaturation = new int[]{RPrefs.getInt(MONET_ACCENT_SATURATION, 100)};
    private final int[] monetBackgroundSaturation = new int[]{RPrefs.getInt(MONET_BACKGROUND_SATURATION, 100)};
    private final int[] monetBackgroundLightness = new int[]{RPrefs.getInt(MONET_BACKGROUND_LIGHTNESS, 100)};
    private final String[][] colorNames = ColorUtil.getColorNames();
    private int[] monetSeedColor;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStylingBinding.inflate(inflater, container, false);

        colorTableRows = new LinearLayout[]{
                binding.colorPreview.systemAccent1,
                binding.colorPreview.systemAccent2,
                binding.colorPreview.systemAccent3,
                binding.colorPreview.systemNeutral1,
                binding.colorPreview.systemNeutral2
        };

        monetSeedColor = new int[]{RPrefs.getInt(
                MONET_SEED_COLOR,
                WallpaperUtil.getWallpaperColor(requireContext())
        )};

        initColorTablePreview(colorTableRows);

        // Primary color
        binding.seedColorPicker.setPreviewColor(RPrefs.getInt(
                MONET_SEED_COLOR,
                WallpaperUtil.getWallpaperColor(requireContext())
        ));
        binding.seedColorPicker.setOnClickListener(v -> new ColorPickerDialog()
                .withCornerRadius(10)
                .withColor(monetSeedColor[0])
                .withAlphaEnabled(false)
                .withPicker(ImagePickerView.class)
                .withListener((pickerView, color) -> {
                    if (monetSeedColor[0] != color) {
                        monetSeedColor[0] = color;
                        binding.seedColorPicker.setPreviewColor(color);
                        RPrefs.putInt(MONET_SEED_COLOR, monetSeedColor[0]);
                        RPrefs.putLong(MONET_LAST_UPDATED, System.currentTimeMillis());
                        new Handler(Looper.getMainLooper()).postDelayed(() -> OverlayManager.applyFabricatedColors(generateModifiedColors()), 800);
                    }
                })
                .show(getChildFragmentManager(), "seedColorPicker")
        );
        binding.seedColorPicker.setVisibility(
                RPrefs.getBoolean(MONET_SEED_COLOR_ENABLED, false) ?
                        View.VISIBLE :
                        View.GONE
        );

        // Monet Style
        int selectedIndex = Arrays.asList(getResources().getStringArray(R.array.monet_style))
                .indexOf(RPrefs.getString(MONET_STYLE, getString(R.string.monet_tonalspot)));
        binding.monetStyles.setSelectedIndex(selectedIndex);
        binding.monetStyles.setOnItemSelectedListener(index -> {
            RPrefs.putString(MONET_STYLE, getResources().getStringArray(R.array.monet_style)[index]);
            ArrayList<ArrayList<Integer>> modifiedColors = generateModifiedColors();
            updatePreviewColors(
                    colorTableRows,
                    modifiedColors
            );
            RPrefs.putLong(MONET_LAST_UPDATED, System.currentTimeMillis());
            new Handler(Looper.getMainLooper()).postDelayed(() -> OverlayManager.applyFabricatedColors(modifiedColors), 800);
        });

        // Monet primary accent saturation
        binding.accentSaturation.setSliderValue(RPrefs.getInt(MONET_ACCENT_SATURATION, 100));

        binding.accentSaturation.setOnSliderChangeListener((slider, value, fromUser) -> {
            monetAccentSaturation[0] = (int) value;
            updatePreviewColors(
                    colorTableRows,
                    generateModifiedColors()
            );
        });

        binding.accentSaturation.setOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                monetAccentSaturation[0] = (int) slider.getValue();
                RPrefs.putInt(MONET_ACCENT_SATURATION, monetAccentSaturation[0]);
                RPrefs.putLong(MONET_LAST_UPDATED, System.currentTimeMillis());
                new Handler(Looper.getMainLooper()).postDelayed(() -> OverlayManager.applyFabricatedColors(generateModifiedColors()), 200);
            }
        });

        // Long Click Reset
        binding.accentSaturation.setResetClickListener(v -> {
            monetAccentSaturation[0] = 100;
            ArrayList<ArrayList<Integer>> modifiedColors = generateModifiedColors();
            updatePreviewColors(
                    colorTableRows,
                    modifiedColors
            );
            RPrefs.clearPref(MONET_ACCENT_SATURATION);
            new Handler(Looper.getMainLooper()).postDelayed(() -> OverlayManager.applyFabricatedColors(modifiedColors), 200);
            return true;
        });

        // Monet background saturation
        binding.backgroundSaturation.setSliderValue(RPrefs.getInt(MONET_BACKGROUND_SATURATION, 100));

        binding.backgroundSaturation.setOnSliderChangeListener((slider, value, fromUser) -> {
            monetBackgroundSaturation[0] = (int) value;
            updatePreviewColors(
                    colorTableRows,
                    generateModifiedColors()
            );
        });

        binding.backgroundSaturation.setOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                monetBackgroundSaturation[0] = (int) slider.getValue();
                RPrefs.putInt(MONET_BACKGROUND_SATURATION, monetBackgroundSaturation[0]);
                RPrefs.putLong(MONET_LAST_UPDATED, System.currentTimeMillis());
                new Handler(Looper.getMainLooper()).postDelayed(() -> OverlayManager.applyFabricatedColors(generateModifiedColors()), 200);
            }
        });

        // Reset button
        binding.backgroundSaturation.setResetClickListener(v -> {
            monetBackgroundSaturation[0] = 100;
            ArrayList<ArrayList<Integer>> modifiedColors = generateModifiedColors();
            updatePreviewColors(
                    colorTableRows,
                    modifiedColors
            );
            RPrefs.clearPref(MONET_BACKGROUND_SATURATION);
            new Handler(Looper.getMainLooper()).postDelayed(() -> OverlayManager.applyFabricatedColors(modifiedColors), 200);
            return true;
        });

        // Monet background lightness
        binding.backgroundLightness.setSliderValue(RPrefs.getInt(MONET_BACKGROUND_LIGHTNESS, 100));

        binding.backgroundLightness.setOnSliderChangeListener((slider, value, fromUser) -> {
            monetBackgroundLightness[0] = (int) value;
            updatePreviewColors(
                    colorTableRows,
                    generateModifiedColors()
            );
        });

        binding.backgroundLightness.setOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                monetBackgroundLightness[0] = (int) slider.getValue();
                RPrefs.putInt(MONET_BACKGROUND_LIGHTNESS, monetBackgroundLightness[0]);
                RPrefs.putLong(MONET_LAST_UPDATED, System.currentTimeMillis());
                new Handler(Looper.getMainLooper()).postDelayed(() -> OverlayManager.applyFabricatedColors(generateModifiedColors()), 200);
            }
        });

        // Long Click Reset
        binding.backgroundLightness.setResetClickListener(v -> {
            monetBackgroundLightness[0] = 100;
            ArrayList<ArrayList<Integer>> modifiedColors = generateModifiedColors();
            updatePreviewColors(
                    colorTableRows,
                    modifiedColors
            );
            RPrefs.clearPref(MONET_BACKGROUND_LIGHTNESS);
            new Handler(Looper.getMainLooper()).postDelayed(() -> OverlayManager.applyFabricatedColors(modifiedColors), 200);
            return true;
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel.getBooleanStates().observe(getViewLifecycleOwner(), this::updateBooleanStates);
        sharedViewModel.getVisibilityStates().observe(getViewLifecycleOwner(), this::updateViewVisibility);
    }

    private void initColorTablePreview(LinearLayout[] colorTableRows) {
        ArrayList<ArrayList<Integer>> palette = generateModifiedColors();

        int[][] systemColors = palette == null ?
                ColorUtil.getSystemColors(requireContext()) :
                MiscUtil.convertListToIntArray(palette);

        for (int i = 0; i < colorTableRows.length; i++) {
            for (int j = 0; j < colorTableRows[i].getChildCount(); j++) {
                colorTableRows[i].getChildAt(j).getBackground().setTint(systemColors[i][j]);
                colorTableRows[i].getChildAt(j).setTag(systemColors[i][j]);

                if (RPrefs.getInt(colorNames[i][j], -1) != -1) {
                    colorTableRows[i].getChildAt(j).getBackground().setTint(RPrefs.getInt(colorNames[i][j], 0));
                }

                TextView textView = new TextView(requireContext());
                textView.setText(String.valueOf(colorCodes[j]));
                textView.setRotation(270);
                textView.setTextColor(ColorUtil.calculateTextColor(systemColors[i][j]));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                textView.setAlpha(0.8f);
                textView.setMaxLines(1);
                textView.setSingleLine(true);
                textView.setAutoSizeTextTypeUniformWithConfiguration(
                        1,
                        20,
                        1,
                        TypedValue.COMPLEX_UNIT_SP
                );

                ((ViewGroup) colorTableRows[i].getChildAt(j)).addView(textView);
                ((LinearLayout) colorTableRows[i].getChildAt(j)).setGravity(Gravity.CENTER);
            }
        }

        enablePaletteOnClickListener(colorTableRows, systemColors);
    }

    private void enablePaletteOnClickListener(LinearLayout[] colorTableRows, int[][] systemColors) {

        for (int i = 0; i < colorTableRows.length; i++) {
            for (int j = 0; j < colorTableRows[i].getChildCount(); j++) {
                int finalI = i;
                int finalJ = j;

                colorTableRows[i].getChildAt(j).setOnClickListener(v -> {
                    boolean manualOverride = RPrefs.getBoolean(MANUAL_OVERRIDE_COLORS, false);
                    String snackbarButton = getString(manualOverride ? R.string.override : R.string.copy);

                    Snackbar.make(
                                    requireView(),
                                    getString(R.string.color_code, ColorUtil.intToHexColor((Integer) v.getTag())),
                                    Snackbar.LENGTH_INDEFINITE)
                            .setAction(snackbarButton, v1 -> {
                                if (!manualOverride) {
                                    ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText(ColorUtil.getColorNames()[finalI][finalJ], ColorUtil.intToHexColor((Integer) v.getTag()));
                                    clipboard.setPrimaryClip(clip);
                                    return;
                                }

                                if (finalJ == 0 || finalJ == 12) {
                                    Snackbar.make(
                                                    requireView(),
                                                    getString(R.string.cannot_override_color),
                                                    Snackbar.LENGTH_SHORT
                                            )
                                            .setAction(getString(R.string.override), v2 -> {
                                            })
                                            .show();
                                    return;
                                }

                                new ColorPickerDialog()
                                        .withCornerRadius(10)
                                        .withColor((Integer) v.getTag())
                                        .withAlphaEnabled(false)
                                        .withPicker(ImagePickerView.class)
                                        .withListener((pickerView, color) -> {
                                            if ((Integer) v.getTag() != color) {
                                                v.setTag(color);
                                                v.getBackground().setTint(color);
                                                ((TextView) ((ViewGroup) v)
                                                        .getChildAt(0))
                                                        .setTextColor(ColorUtil.calculateTextColor(color));
                                                RPrefs.putInt(colorNames[finalI][finalJ], color);
                                            }
                                        })
                                        .show(getChildFragmentManager(), "overrideColorPicker" + finalI + finalJ);
                            })
                            .show();
                });

                colorTableRows[i].getChildAt(j).setOnLongClickListener(v -> {
                    if (finalJ == 0 || finalJ == 12) {
                        return true;
                    }

                    RPrefs.clearPref(colorNames[finalI][finalJ]);
                    v.getBackground().setTint(systemColors[finalI][finalJ]);
                    v.setTag(systemColors[finalI][finalJ]);
                    ((TextView) ((ViewGroup) v)
                            .getChildAt(0))
                            .setTextColor(ColorUtil.calculateTextColor(systemColors[finalI][finalJ]));
                    Snackbar.make(
                                    requireView(),
                                    getString(R.string.color_reset_success),
                                    Snackbar.LENGTH_SHORT
                            )
                            .setAction(getString(R.string.reset_all), v2 -> {
                                for (int x = 0; x < colorTableRows.length; x++) {
                                    for (int y = 0; y < colorTableRows[x].getChildCount(); y++) {
                                        RPrefs.clearPref(colorNames[x][y]);
                                    }
                                }
                                Snackbar.make(
                                                requireView(),
                                                getString(R.string.reset_all_success),
                                                Snackbar.LENGTH_SHORT
                                        )
                                        .setAction(getString(R.string.dismiss), v3 -> {
                                        })
                                        .show();
                            })
                            .show();
                    return true;
                });
            }
        }
    }

    private void updateBooleanStates(Map<String, Boolean> stringBooleanMap) {
        Boolean accurateShades = stringBooleanMap.get(MONET_ACCURATE_SHADES);
        if (accurateShades != null) {
            updatePreviewColors(
                    colorTableRows,
                    generateModifiedColors()
            );
        }
    }

    private void updateViewVisibility(Map<String, Integer> visibilityStates) {
        Integer seedColorVisibility = visibilityStates.get(MONET_SEED_COLOR_ENABLED);
        if (seedColorVisibility != null && binding.seedColorPicker.getVisibility() != seedColorVisibility) {
            binding.seedColorPicker.setVisibility(seedColorVisibility);

            if (seedColorVisibility == View.GONE) {
                if (RPrefs.getInt(MONET_SEED_COLOR, -1) != -1) {
                    monetSeedColor = new int[]{WallpaperUtil.getWallpaperColor(requireContext())};
                    binding.seedColorPicker.setPreviewColor(monetSeedColor[0]);
                    RPrefs.clearPref(MONET_SEED_COLOR);
                    RPrefs.putLong(MONET_LAST_UPDATED, System.currentTimeMillis());
                    new Handler(Looper.getMainLooper()).postDelayed(() -> OverlayManager.applyFabricatedColors(generateModifiedColors()), 800);
                }
            } else {
                monetSeedColor = new int[]{RPrefs.getInt(
                        MONET_SEED_COLOR,
                        WallpaperUtil.getWallpaperColor(requireContext())
                )};
                binding.seedColorPicker.setPreviewColor(monetSeedColor[0]);
            }
        }
    }

    private @ColorInt int getPrimaryColor() {
        return requireContext().getColor(com.google.android.material.R.color.material_dynamic_primary40);
    }

    private void updatePreviewColors(LinearLayout[] colorTableRows, ArrayList<ArrayList<Integer>> palette) {
        if (palette == null) return;

        // Update preview colors
        for (int i = 0; i < colorTableRows.length; i++) {
            for (int j = 0; j < colorTableRows[i].getChildCount(); j++) {
                colorTableRows[i].getChildAt(j).getBackground().setTint(palette.get(i).get(j));
                colorTableRows[i].getChildAt(j).setTag(palette.get(i).get(j));
                ((TextView) ((ViewGroup) colorTableRows[i].getChildAt(j))
                        .getChildAt(0))
                        .setTextColor(ColorUtil.calculateTextColor(palette.get(i).get(j)));
            }
        }
    }

    private ArrayList<ArrayList<Integer>> generateModifiedColors() {
        try {
            return ColorUtil.generateModifiedColors(
                    requireContext(),
                    ColorSchemeUtil.stringToEnum(
                            requireContext(),
                            RPrefs.getString(MONET_STYLE, getString(R.string.monet_tonalspot))
                    ),
                    monetAccentSaturation[0],
                    monetBackgroundSaturation[0],
                    monetBackgroundLightness[0],
                    RPrefs.getBoolean(MONET_PITCH_BLACK_THEME, false),
                    RPrefs.getBoolean(MONET_ACCURATE_SHADES, true)
            );
        } catch (Exception e) {
            Log.e(TAG, "Error generating modified colors", e);
            return null;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }
}