package com.duoc.bank_batch.config;

import com.duoc.bank_batch.entity.Cuenta;
import com.duoc.bank_batch.entity.Interes;
import com.duoc.bank_batch.entity.Transaccion;

import com.duoc.bank_batch.processor.TransaccionProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class BatchConfig {

    // ============================================================
    // JOB
    // ============================================================

    @Bean
    public Job bankJob(
            JobRepository jobRepository,
            Step cuentasStep,
            Step interesesStep,
            Step transaccionesStep) {

        return new JobBuilder("bankJob", jobRepository)
                .incrementer(new org.springframework.batch.core.launch.support.RunIdIncrementer())
                .start(cuentasStep)
                .next(interesesStep)
                .next(transaccionesStep)
                .build();
    }

    // ============================================================
    // STEP CUENTAS
    // ============================================================

    @Bean
    public Step cuentasStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<Cuenta> cuentasReader,
            ItemProcessor<Cuenta, Cuenta> cuentasProcessor,
            JpaItemWriter<Cuenta> cuentasWriter) {

        return new StepBuilder("cuentasStep", jobRepository)
                .<Cuenta, Cuenta>chunk(10, transactionManager)
                .reader(cuentasReader)
                .processor(cuentasProcessor)
                .writer(cuentasWriter)
                .build();
    }

    // ============================================================
    // READER CUENTAS
    // ============================================================

    @Bean
    public FlatFileItemReader<Cuenta> cuentasReader() {

        return new FlatFileItemReaderBuilder<Cuenta>()
                .name("cuentasReader")
                .resource(
                        new ClassPathResource("data/cuentas_anuales.csv")
                )
                .linesToSkip(1)
                .lineMapper((line, lineNumber) -> {

                    String[] values = line.split(",", -1);

                    Cuenta cuenta = new Cuenta();

                    cuenta.setCuentaId(
                            Long.parseLong(values[0])
                    );

                    cuenta.setFecha(
                            LocalDate.parse(values[1])
                    );

                    cuenta.setTransaccion(
                            values[2]
                    );

                    cuenta.setMonto(
                            new BigDecimal(values[3])
                    );

                    cuenta.setDescripcion(
                            values[4]
                    );

                    return cuenta;
                })
                .build();
    }

    // ============================================================
    // PROCESSOR CUENTAS
    // ============================================================

    @Bean
    public ItemProcessor<Cuenta, Cuenta> cuentasProcessor() {
        return cuenta -> cuenta;
    }

    // ============================================================
    // WRITER CUENTAS
    // ============================================================

    @Bean
    public JpaItemWriter<Cuenta> cuentasWriter(
            jakarta.persistence.EntityManagerFactory entityManagerFactory) {

        JpaItemWriter<Cuenta> writer = new JpaItemWriter<>();

        writer.setEntityManagerFactory(
                entityManagerFactory
        );

        return writer;
    }

    // ============================================================
    // STEP INTERESES
    // ============================================================

    @Bean
    public Step interesesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<Interes> interesesReader,
            ItemProcessor<Interes, Interes> interesesProcessor,
            JpaItemWriter<Interes> interesesWriter) {

        return new StepBuilder("interesesStep", jobRepository)
                .<Interes, Interes>chunk(10, transactionManager)
                .reader(interesesReader)
                .processor(interesesProcessor)
                .writer(interesesWriter)
                .build();
    }

    // ============================================================
    // READER INTERESES
    // ============================================================

    @Bean
    public FlatFileItemReader<Interes> interesesReader() {

        return new FlatFileItemReaderBuilder<Interes>()
                .name("interesesReader")
                .resource(
                        new ClassPathResource("data/intereses.csv")
                )
                .linesToSkip(1)
                .lineMapper((line, lineNumber) -> {

                    String[] values = line.split(",", -1);

                    Interes interes = new Interes();

                    interes.setCuentaId(Long.parseLong(values[0]));
                    interes.setNombre(values[1]);
                    interes.setSaldo(new BigDecimal(values[2]));
                    interes.setEdad(Integer.parseInt(values[3]));
                    interes.setTipo(values[4]);

                    return interes;
                })
                .build();
    }

    // ============================================================
    // PROCESSOR INTERESES
    // ============================================================

    @Bean
    public ItemProcessor<Interes, Interes> interesesProcessor() {

        return interes -> {

            // Fecha de procesamiento
            interes.setFecha(LocalDate.now());

            // Tasa de interés de ejemplo: 5%
            BigDecimal tasa = new BigDecimal("0.05");

            // Cálculo del interés
            BigDecimal montoInteres = interes.getSaldo()
                    .multiply(tasa);

            interes.setMonto(montoInteres);

            return interes;
        };
    }

    // ============================================================
    // WRITER INTERESES
    // ============================================================

    @Bean
    public JpaItemWriter<Interes> interesesWriter(
            jakarta.persistence.EntityManagerFactory entityManagerFactory) {

        JpaItemWriter<Interes> writer = new JpaItemWriter<>();

        writer.setEntityManagerFactory(
                entityManagerFactory
        );

        return writer;
    }

// ============================================================
    // READER TRANSACCIONES
    // ============================================================

    @Bean
    public FlatFileItemReader<Transaccion> transaccionesReader() {

        return new FlatFileItemReaderBuilder<Transaccion>()
                .name("transaccionesReader")
                .resource(
                        new ClassPathResource("data/transacciones.csv")
                )
                .linesToSkip(1)
                .lineMapper((line, lineNumber) -> {

                    String[] values = line.split(",", -1);

                    Transaccion transaccion = new Transaccion();

                    transaccion.setFecha(LocalDate.parse(values[1]));
                    transaccion.setMonto(new BigDecimal(values[2]));
                    transaccion.setTipo(values[3]);

                    return transaccion;
                })
                .build();
    }

// ============================================================
    // PROCESSOR TRANSACCIONES
    // ============================================================

    @Bean
    public TransaccionProcessor transaccionesProcessor() {
        return new TransaccionProcessor();
    }

// ============================================================
    // STEP TRANSACCIONES
    // ============================================================

    @Bean
    public Step transaccionesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<Transaccion> transaccionesReader,
            TransaccionProcessor transaccionesProcessor,
            JpaItemWriter<Transaccion> transaccionesWriter) {

        return new StepBuilder("transaccionesStep", jobRepository)
                .<Transaccion, Transaccion>chunk(10, transactionManager)
                .reader(transaccionesReader)
                .processor(transaccionesProcessor)
                .writer(transaccionesWriter)
                .listener((StepExecutionListener) transaccionesProcessor)
                .build();
    }

    // ============================================================
    // WRITER TRANSACCIONES
    // ============================================================

    @Bean
    public JpaItemWriter<Transaccion> transaccionesWriter(
            jakarta.persistence.EntityManagerFactory entityManagerFactory) {

        JpaItemWriter<Transaccion> writer = new JpaItemWriter<>();

        writer.setEntityManagerFactory(
                entityManagerFactory
        );

        return writer;
    }
}