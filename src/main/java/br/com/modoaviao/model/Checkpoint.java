package br.com.modoaviao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "checkpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Checkpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "capitulo_id", unique = true)
    private Capitulo capitulo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String espelhoDeTexto;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String perguntaEscala;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String cardDeBolso;
}
