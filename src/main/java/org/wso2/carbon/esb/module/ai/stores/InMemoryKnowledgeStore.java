package org.wso2.carbon.esb.module.ai.stores;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.wso2.carbon.esb.module.ai.models.TextEmbedding;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;

import java.util.ArrayList;
import java.util.List;

public class InMemoryKnowledgeStore implements KnowledgeStore {

    public static final String AI_KNOWLEDGE_STORE = "ai/knowledge-store/";
    public static final String JSON = ".json";
    public static final String CONTENT_TYPE = "application/json";
    private static MicroIntegratorRegistry registry;

    private final String name;

    private final EmbeddingStore<TextSegment> embeddingStore;

    private final String STORE_FILE;

    public InMemoryKnowledgeStore(String name, MicroIntegratorRegistry registry) {
        this.name = name;
        InMemoryKnowledgeStore.registry = registry;

        STORE_FILE = AI_KNOWLEDGE_STORE + name + JSON;
        if (registry.isResourceExists(STORE_FILE)) {
            this.embeddingStore = InMemoryEmbeddingStore.fromFile(registry.getRegistryEntry(STORE_FILE).getName());
        } else {
            this.embeddingStore = new InMemoryEmbeddingStore<>();
            persistStoreToRegistry();
        }
    }

    @Override
    public void add(List<TextEmbedding> textEmbeddings) {
        List<Embedding> embeddings = new ArrayList<>();
        List<TextSegment> textSegments = new ArrayList<>();

        for (TextEmbedding textEmbedding : textEmbeddings) {
            embeddings.add(new Embedding(textEmbedding.getEmbedding()));
            textSegments.add(new TextSegment(textEmbedding.getText(), textEmbedding.getMetadata()));
        }

        synchronized (this) {
            embeddingStore.addAll(embeddings, textSegments);
            persistStoreToRegistry();
        }
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> search(Embedding embedding, Integer maxResults, Double minScore, Filter filter) {
        EmbeddingSearchRequest embeddingSearchRequest = new EmbeddingSearchRequest(embedding, maxResults, minScore, filter);
        return embeddingStore.search(embeddingSearchRequest).matches();
    }

    private synchronized void persistStoreToRegistry() {
        InMemoryEmbeddingStore<TextSegment> embeddingStore = (InMemoryEmbeddingStore<TextSegment>) this.embeddingStore;
        String serializedStore = embeddingStore.serializeToJson();
        registry.addMultipartResource(STORE_FILE, CONTENT_TYPE, serializedStore.getBytes());
    }
}
