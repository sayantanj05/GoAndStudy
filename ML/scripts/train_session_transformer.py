"""Train Transformer for next-action prediction from session sequences."""
import tensorflow as tf
from tensorflow.keras import layers

MAX_SEQ_LEN = 20
EMBEDDING_DIM = 32

def create_session_model(num_books: int, num_actions: int):
    book_input = layers.Input(shape=(MAX_SEQ_LEN,), name='book_ids')
    action_input = layers.Input(shape=(MAX_SEQ_LEN,), name='action_types')
    
    book_emb = layers.Embedding(num_books, EMBEDDING_DIM)(book_input)
    action_emb = layers.Embedding(num_actions, 4)(action_input)
    
    # Concatenate embeddings
    x = layers.Concatenate()([book_emb, action_emb])
    
    # Transformer encoder
    x = layers.MultiHeadAttention(num_heads=4, key_dim=32)(x, x)
    x = layers.LayerNormalization()(x)
    x = layers.GlobalAveragePooling1D()(x)
    x = layers.Dense(64, activation='relu')(x)
    output = layers.Dense(num_books, activation='softmax', name='next_book')(x)
    
    model = tf.keras.Model(inputs=[book_input, action_input], outputs=output)
    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
    return model

