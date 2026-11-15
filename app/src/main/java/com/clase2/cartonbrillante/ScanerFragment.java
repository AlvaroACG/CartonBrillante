package com.clase2.cartonbrillante;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanerFragment extends Fragment {

    private PreviewView previewCamara;
    private TextView tvCodigoDetectado, tvTipoCodigo;
    private Button btnReescanear;

    private ExecutorService cameraExecutor;
    private TextRecognizer textRecognizer;
    private boolean escaneoPausado = false;

    // Patrón 1: Passcode de Yu-Gi-Oh! (Exactamente 8 dígitos numéricos, ej: 68468459)
    private static final Pattern PATTERN_PASSCODE = Pattern.compile("\\b\\d{8}\\b");

    // Patrón 2: Código de Expansión/Set (ej: ROTD-EN005, BLVO-SP011, LOB-001)
    private static final Pattern PATTERN_SET_CODE = Pattern.compile("\\b[A-Z0-9]{3,4}-[A-Z]{0,2}\\d{3}\\b");

    private final ActivityResultLauncher<String> permisoCamaraLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            concedido -> {
                if (concedido) {
                    iniciarCamara();
                } else {
                    tvCodigoDetectado.setText("Sin Permiso");
                    tvTipoCodigo.setText("Concede permiso de cámara para escanear cartas.");
                    Toast.makeText(getContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scaner, container, false);

        previewCamara = view.findViewById(R.id.previewCamara);
        tvCodigoDetectado = view.findViewById(R.id.tvCodigoDetectado);
        tvTipoCodigo = view.findViewById(R.id.tvTipoCodigo);
        btnReescanear = view.findViewById(R.id.btnReescanear);

        cameraExecutor = Executors.newSingleThreadExecutor();
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        btnReescanear.setOnClickListener(v -> {
            escaneoPausado = false;
            tvCodigoDetectado.setText("Escaneando...");
            tvTipoCodigo.setText("Esperando enfocar numeración...");
        });

        verificarPermisoEIniciarCamara();

        return view;
    }

    private void verificarPermisoEIniciarCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewCamara.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analizarFotograma);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Toast.makeText(getContext(), "Error al iniciar la cámara", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analizarFotograma(@NonNull ImageProxy imageProxy) {
        if (escaneoPausado) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            textRecognizer.process(image)
                    .addOnSuccessListener(visionText -> buscarCodigoEnTexto(visionText))
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void buscarCodigoEnTexto(Text visionText) {
        if (escaneoPausado) return;

        String textoCompleto = visionText.getText();
        if (textoCompleto.isEmpty()) return;

        // 1. Buscar primero el código numérico de 8 dígitos (Passcode inferior izquierdo)
        Matcher matcherPasscode = PATTERN_PASSCODE.matcher(textoCompleto);
        if (matcherPasscode.find()) {
            String codigoNumerico = matcherPasscode.group();
            mostrarCodigoEnPantalla(codigoNumerico, "Passcode Numérico (8 dígitos)");
            return;
        }

        // 2. Buscar también código de expansión (ej: ROTD-EN005)
        Matcher matcherSet = PATTERN_SET_CODE.matcher(textoCompleto.toUpperCase());
        if (matcherSet.find()) {
            String codigoSet = matcherSet.group();
            mostrarCodigoEnPantalla(codigoSet, "Código de Set / Expansión");
        }
    }

    private void mostrarCodigoEnPantalla(String codigo, String descripcionTipo) {
        escaneoPausado = true; // Pausa hasta que el usuario presione "Escanear Otra Carta"
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                tvCodigoDetectado.setText(codigo);
                tvTipoCodigo.setText(descripcionTipo);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }
}