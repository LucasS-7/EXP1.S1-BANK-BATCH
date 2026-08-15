package com.duoc.bank_batch.repository;

import com.duoc.bank_batch.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

}