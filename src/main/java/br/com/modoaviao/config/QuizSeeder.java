package br.com.modoaviao.config;

import br.com.modoaviao.model.ModoFuga;
import br.com.modoaviao.model.QuizOpcao;
import br.com.modoaviao.model.QuizPergunta;
import br.com.modoaviao.model.QuizResultado;
import br.com.modoaviao.repository.QuizOpcaoRepository;
import br.com.modoaviao.repository.QuizPerguntaRepository;
import br.com.modoaviao.repository.QuizResultadoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * SOMENTE DESENVOLVIMENTO - @Profile("dev").
 *
 * Popula o quiz diagnostico "Qual e o seu Modo de Fuga?" completo (10
 * perguntas + opcoes + os 4 textos de resultado) de uma vez, para nao
 * precisar cadastrar tudo manualmente pelo admin. Roda uma vez a cada start
 * da aplicacao e nao faz nada se ja existir alguma QuizPergunta no banco
 * (idempotente).
 * ============================================================================
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class QuizSeeder implements CommandLineRunner {

    private final QuizPerguntaRepository quizPerguntaRepository;
    private final QuizOpcaoRepository quizOpcaoRepository;
    private final QuizResultadoRepository quizResultadoRepository;

    private record OpcaoSeed(String texto, ModoFuga modo) {
    }

    private record PerguntaSeed(int ordem, String enunciado, List<OpcaoSeed> opcoes) {
    }

    private record ResultadoSeed(ModoFuga modo, String titulo, String emoji, String texto) {
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (quizPerguntaRepository.count() > 0) {
            log.info("[DEV-SEED] Quiz ja possui perguntas cadastradas - seeder ignorado.");
            return;
        }

        List<PerguntaSeed> perguntas = perguntas();
        int totalOpcoes = 0;

        for (PerguntaSeed perguntaSeed : perguntas) {
            QuizPergunta pergunta = new QuizPergunta();
            pergunta.setOrdem(perguntaSeed.ordem());
            pergunta.setEnunciado(perguntaSeed.enunciado());
            pergunta = quizPerguntaRepository.save(pergunta);

            int ordemOpcao = 1;
            for (OpcaoSeed opcaoSeed : perguntaSeed.opcoes()) {
                QuizOpcao opcao = new QuizOpcao();
                opcao.setPergunta(pergunta);
                opcao.setTexto(opcaoSeed.texto());
                opcao.setModo(opcaoSeed.modo());
                opcao.setOrdem(ordemOpcao++);
                quizOpcaoRepository.save(opcao);
                totalOpcoes++;
            }
        }

        List<ResultadoSeed> resultados = resultados();
        for (ResultadoSeed resultadoSeed : resultados) {
            QuizResultado resultado = new QuizResultado();
            resultado.setModo(resultadoSeed.modo());
            resultado.setTitulo(resultadoSeed.titulo());
            resultado.setEmoji(resultadoSeed.emoji());
            resultado.setTexto(resultadoSeed.texto());
            quizResultadoRepository.save(resultado);
        }

        log.warn("[DEV-SEED] Quiz inserido -> {} perguntas, {} opcoes, {} resultados",
                perguntas.size(), totalOpcoes, resultados.size());
    }

    private List<PerguntaSeed> perguntas() {
        return List.of(
                new PerguntaSeed(1, "As coisas começam a esfriar com alguém. Na prática, o que você faz?", List.of(
                        new OpcaoSeed("Vou respondendo cada vez menos, até simplesmente parar de responder.", ModoFuga.FANTASMA),
                        new OpcaoSeed("Começo a reparar em defeitos que antes não me incomodavam e a implicar.", ModoFuga.SABOTADOR),
                        new OpcaoSeed("Percebo que a pessoa não é bem o que eu imaginava e perco o interesse.", ModoFuga.IDEALIZADOR),
                        new OpcaoSeed("Nem chega a esfriar de verdade — eu já estava conversando com outras pessoas.", ModoFuga.COLECIONADOR))),

                new PerguntaSeed(2, "Qual dessas frases mais sai da sua boca?", List.of(
                        new OpcaoSeed("Prefiro não criar drama, sumo e pronto.", ModoFuga.FANTASMA),
                        new OpcaoSeed("Eu só não me contento com pouco, quero o amor de verdade.", ModoFuga.IDEALIZADOR),
                        new OpcaoSeed("Tô numa fase de conhecer gente, sem me prender a ninguém.", ModoFuga.COLECIONADOR),
                        new OpcaoSeed("Relação sem intensidade pra mim é sinal de que esfriou.", ModoFuga.SABOTADOR))),

                new PerguntaSeed(3, "Uma relação está indo bem, calma, sem problema nenhum. Como você se sente?", List.of(
                        new OpcaoSeed("Ótimo, é isso que eu procuro.", null),
                        new OpcaoSeed("Meio inquieto, como se faltasse emoção, como se tivesse bom demais.", ModoFuga.SABOTADOR),
                        new OpcaoSeed("Começo a achar meio sem graça, sinto falta do friozinho do começo.", ModoFuga.IDEALIZADOR),
                        new OpcaoSeed("Confortável, mas mantenho outras conversas rolando de qualquer forma.", ModoFuga.COLECIONADOR),
                        new OpcaoSeed("Bem, mas dá um medo de me entregar e depois ter que encarar o fim.", ModoFuga.FANTASMA))),

                new PerguntaSeed(4, "Você olha pra trás e vê um histórico de:", List.of(
                        new OpcaoSeed("Pessoas que ficaram sem entender por que você sumiu.", ModoFuga.FANTASMA),
                        new OpcaoSeed("Relações que desandaram justo quando estavam ficando boas.", ModoFuga.SABOTADOR),
                        new OpcaoSeed("Paixões intensas que morreram quando a pessoa \"mudou\".", ModoFuga.IDEALIZADOR),
                        new OpcaoSeed("Vários lances sobrepostos, nenhum que ficou realmente fundo.", ModoFuga.COLECIONADOR))),

                new PerguntaSeed(5, "Quando alguém começa a te cobrar uma definição (\"o que a gente é?\"), você:", List.of(
                        new OpcaoSeed("Some ou enrola até a pessoa desistir de perguntar.", ModoFuga.FANTASMA),
                        new OpcaoSeed("Fico irritado, acho a cobrança um problema, começo a me afastar.", ModoFuga.SABOTADOR),
                        new OpcaoSeed("Uso como prova de que \"não era isso mesmo\" e sigo em frente.", ModoFuga.IDEALIZADOR),
                        new OpcaoSeed("Digo que tô \"só curtindo\" e lembro que tenho outras opções.", ModoFuga.COLECIONADOR))),

                new PerguntaSeed(6, "A pessoa que mais te marcou foi:", List.of(
                        new OpcaoSeed("Alguém que eu deixei escapar e de quem sumi sem explicação.", ModoFuga.FANTASMA),
                        new OpcaoSeed("Alguém ótimo, que eu afastei sem entender bem por quê.", ModoFuga.SABOTADOR),
                        new OpcaoSeed("Um ex idealizado ou alguém inalcançável que eu nunca tive de verdade.", ModoFuga.IDEALIZADOR),
                        new OpcaoSeed("Difícil dizer — foram muitos, nenhum tempo suficiente pra marcar.", ModoFuga.COLECIONADOR))),

                new PerguntaSeed(7, "O que mais te dá medo num relacionamento?", List.of(
                        new OpcaoSeed("Ter que encarar uma conversa difícil, um confronto, uma reação.", ModoFuga.FANTASMA),
                        new OpcaoSeed("Baixar a guarda, relaxar, e ser pego de surpresa pela queda.", ModoFuga.SABOTADOR),
                        new OpcaoSeed("Descobrir que a pessoa é comum e que eu me enganei.", ModoFuga.IDEALIZADOR),
                        new OpcaoSeed("Apostar tudo numa pessoa só e ela ir embora com tudo.", ModoFuga.COLECIONADOR))),

                new PerguntaSeed(8, "Na fase de novidade, quando tudo é empolgante, você:", List.of(
                        new OpcaoSeed("Curto, mas já fico com um pé atrás sabendo que vou querer sumir depois.", ModoFuga.FANTASMA),
                        new OpcaoSeed("Curto até demais — e é logo depois que começo a estragar.", ModoFuga.SABOTADOR),
                        new OpcaoSeed("Vivo intensamente, essa é a minha fase favorita, é quando amo mais.", ModoFuga.IDEALIZADOR),
                        new OpcaoSeed("Curto, mas nunca só com uma pessoa por vez.", ModoFuga.COLECIONADOR))),

                new PerguntaSeed(9, "Quando uma pessoa começa a importar de verdade, sua reação é:", List.of(
                        new OpcaoSeed("Sentir vontade de recuar e criar distância.", ModoFuga.FANTASMA),
                        new OpcaoSeed("Ficar tenso e começar a achar defeito pra diminuir o peso.", ModoFuga.SABOTADOR),
                        new OpcaoSeed("Ter medo de que ela estrague a imagem perfeita que eu criei.", ModoFuga.IDEALIZADOR),
                        new OpcaoSeed("Reabrir o app ou puxar outra conversa \"só pra não ficar exposto\".", ModoFuga.COLECIONADOR))),

                new PerguntaSeed(10, "Se você fosse brutalmente honesto, o seu problema é que você:", List.of(
                        new OpcaoSeed("Foge do confronto e prefere desaparecer a encarar.", ModoFuga.FANTASMA),
                        new OpcaoSeed("Destrói o que é bom porque não confio que vá durar.", ModoFuga.SABOTADOR),
                        new OpcaoSeed("Amo uma fantasia e me decepciono com gente real.", ModoFuga.IDEALIZADOR),
                        new OpcaoSeed("Nunca me entrego inteiro porque sempre tenho um plano B.", ModoFuga.COLECIONADOR))));
    }

    private List<ResultadoSeed> resultados() {
        return List.of(
                new ResultadoSeed(ModoFuga.FANTASMA, "O Fantasma", "👻",
                        "Você foge saindo — mas sem avisar. Quando aperta, você evapora: para de responder, some, "
                                + "deixa a pessoa montando sozinha o quebra-cabeça. Não é maldade, é pavor de confronto. "
                                + "Você aprendeu que desaparecer é mais fácil que encarar uma conversa difícil. O seu "
                                + "capítulo é o 4, e ele vai te mostrar por que sumir nunca foi a gentileza que você diz "
                                + "que é — e qual é o primeiro passo pra parar."),

                new ResultadoSeed(ModoFuga.SABOTADOR, "O Sabotador", "💣",
                        "Você não some — você quebra. Quando a relação fica boa demais, algo em você entra em pânico "
                                + "e começa a destruir de dentro: briga do nada, frieza súbita, defeito onde não tinha. "
                                + "Paz te assusta mais que caos, porque você aprendeu que calmaria é a véspera da queda. "
                                + "O seu capítulo é o 5, e ele vai te mostrar como reconhecer a sua própria mão antes de "
                                + "ela apertar o gatilho."),

                new ResultadoSeed(ModoFuga.IDEALIZADOR, "O Idealizador", "🎭",
                        "Você foge sem nunca ter chegado. Está sempre apaixonado — mas por uma fantasia, não por uma "
                                + "pessoa. Quando o outro finalmente aparece, humano e comum, você sente decepção e vai "
                                + "embora procurar o próximo ideal. O seu capítulo é o 6, e ele vai te mostrar que o "
                                + "momento em que a pessoa \"perde a graça\" é exatamente onde o amor de verdade começa."),

                new ResultadoSeed(ModoFuga.COLECIONADOR, "O Colecionador", "🎴",
                        "Você foge estando sempre acompanhado. Nunca fica sozinho, mas também nunca se entrega — "
                                + "mantém várias frentes abertas pra que ninguém tenha poder de te machucar. Chama isso "
                                + "de liberdade; é armadura. O seu capítulo é o 7, e ele vai te mostrar por que apostar "
                                + "tudo em uma pessoa só é a única forma de finalmente ser conhecido."));
    }
}
