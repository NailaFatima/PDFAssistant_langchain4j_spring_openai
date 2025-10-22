package com.example.langchain4pdfchat;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.cassandra.AstraDbEmbeddingConfiguration;
import dev.langchain4j.store.embedding.cassandra.AstraDbEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PdfAssistantConfig {

    // Injecting values from application.properties
    @Value("${astra.db.token}")
    private String astraToken;

    @Value("${astra.db.id}")
    private String databaseId;

    @Value("${astra.region}")
    private String region;

    @Value("${astra.keyspace}")
    private String keyspace;

    @Value("${astra.table}")
    private String table;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    // Embedding Model Bean
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    // Astra DB Embedding Store Bean
    @Bean
    public AstraDbEmbeddingStore astraDbEmbeddingStore() {
        AstraDbEmbeddingConfiguration config = AstraDbEmbeddingConfiguration.builder()
                .token(astraToken)
                .databaseId(databaseId)
                .databaseRegion(region)
                .keyspace(keyspace)
                .table(table)
                .dimension(384)
                .build();

        return new AstraDbEmbeddingStore(config);
    }

    // Ingestor Bean for splitting and storing embeddings
    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor() {
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel())
                .embeddingStore(astraDbEmbeddingStore())
                .build();
    }

    // Conversational Retrieval Chain Bean
    @Bean
    public ConversationalRetrievalChain conversationalRetrievalChain() {
        return ConversationalRetrievalChain.builder()
                .chatLanguageModel(OpenAiChatModel.withApiKey(openAiApiKey))
                .retriever(EmbeddingStoreRetriever.from(astraDbEmbeddingStore(), embeddingModel()))
                .build();
    }
}
