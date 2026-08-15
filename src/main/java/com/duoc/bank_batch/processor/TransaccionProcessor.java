package com.duoc.bank_batch.processor;

import com.duoc.bank_batch.entity.Transaccion;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class TransaccionProcessor implements ItemProcessor<Transaccion, Transaccion>, StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(TransaccionProcessor.class);

    private int totalLeidas = 0;
    private int montoCero = 0;
    private int tipoCorregido = 0;
    private int tipoInvalido = 0;
    private int duplicados = 0;

    private final Set<String> clavesVistas = new HashSet<>();

    @Override
    public Transaccion process(Transaccion transaccion) {

        totalLeidas++;

        // --- Normalización del campo 'tipo' ---
        String tipoOriginal = transaccion.getTipo();
        String tipoNormalizado = tipoOriginal == null ? "" : tipoOriginal.trim().toLowerCase();

        if (!tipoNormalizado.equals(tipoOriginal)) {
            tipoCorregido++;
        }

        // --- Validación: tipo debe ser 'debito' o 'credito' ---
        if (!tipoNormalizado.equals("debito") && !tipoNormalizado.equals("credito")) {
            tipoInvalido++;
            log.warn("Transacción con tipo inválido descartada: fecha={}, monto={}, tipo='{}'",
                    transaccion.getFecha(), transaccion.getMonto(), tipoOriginal);
            return null; // se descarta: dato mal clasificado, no se puede insertar con confianza
        }

        transaccion.setTipo(tipoNormalizado);

        // --- Anomalía: monto en cero (se registra pero NO se descarta) ---
        if (transaccion.getMonto() != null && transaccion.getMonto().signum() == 0) {
            montoCero++;
            log.warn("Anomalía detectada - monto en cero: fecha={}, tipo={}",
                    transaccion.getFecha(), transaccion.getTipo());
        }

        // --- Anomalía: posible duplicado exacto (se registra pero NO se descarta) ---
        String clave = transaccion.getFecha() + "|" + transaccion.getMonto() + "|" + transaccion.getTipo();
        if (!clavesVistas.add(clave)) {
            duplicados++;
            log.warn("Anomalía detectada - posible duplicado: fecha={}, monto={}, tipo={}",
                    transaccion.getFecha(), transaccion.getMonto(), transaccion.getTipo());
        }

        return transaccion;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // reinicia contadores por si el step se reintenta
        totalLeidas = 0;
        montoCero = 0;
        tipoCorregido = 0;
        tipoInvalido = 0;
        duplicados = 0;
        clavesVistas.clear();
    }

    @Override
    public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {

        log.info("=========== RESUMEN - REPORTE DE TRANSACCIONES DIARIAS ===========");
        log.info("Total de registros leídos:              {}", totalLeidas);
        log.info("Registros con tipo normalizado (trim/lower): {}", tipoCorregido);
        log.info("Registros descartados por tipo inválido: {}", tipoInvalido);
        log.info("Anomalías - monto en cero:               {}", montoCero);
        log.info("Anomalías - posibles duplicados:         {}", duplicados);
        log.info("Registros efectivamente insertados:      {}", stepExecution.getWriteCount());
        log.info("====================================================================");

        return stepExecution.getExitStatus();
    }
}