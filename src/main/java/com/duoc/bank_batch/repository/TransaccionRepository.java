package com.duoc.bank_batch.repository;

import com.duoc.bank_batch.entity.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

}
