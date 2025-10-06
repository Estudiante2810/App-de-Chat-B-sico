package com.example.chatbasico;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Message {
    
    // Constantes para tipos de mensaje
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    
    // Campos del mensaje
    private String id;
    private String text;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String receiverName;
    @ServerTimestamp
    private Date timestamp;
    private String conversationId;
    
    // Nuevos campos para imágenes
    private String messageType; // "text" o "image"
    private String imageUrl;    // URL de la imagen en Firebase Storage
    private String imageName;   // Nombre del archivo de imagen
    
    // Constructor vacío requerido por Firestore
    public Message() {
        this.messageType = TYPE_TEXT; // Por defecto es texto
    }
    
    // Constructor para mensaje de texto
    public Message(String text, String senderId, String senderName, 
                   String receiverId, String receiverName, String conversationId) {
        this.text = text;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.conversationId = conversationId;
        this.messageType = TYPE_TEXT;
        this.timestamp = null; // Se asignará automáticamente por Firebase
    }
    
    // Constructor para mensaje de imagen
    public Message(String senderId, String senderName, String receiverId, 
                   String receiverName, String conversationId, String imageUrl, String imageName) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.conversationId = conversationId;
        this.imageUrl = imageUrl;
        this.imageName = imageName;
        this.messageType = TYPE_IMAGE;
        this.text = ""; // Mensaje de imagen no tiene texto
        this.timestamp = null; // Se asignará automáticamente por Firebase
    }
    
    // Getters y Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getReceiverName() {
        return receiverName;
    }
    
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    // Getters y Setters para campos de imagen
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getImageName() {
        return imageName;
    }
    
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
    
    // Métodos de utilidad
    public boolean isImageMessage() {
        return TYPE_IMAGE.equals(messageType);
    }
    
    public boolean isTextMessage() {
        return TYPE_TEXT.equals(messageType);
    }
    
    // Método para generar un ID de conversación único entre dos usuarios
    public static String generateConversationId(String userId1, String userId2) {
        // Ordenamos los IDs alfabéticamente para garantizar que el ID sea consistente
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", senderId='" + senderId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", receiverName='" + receiverName + '\'' +
                ", timestamp=" + timestamp +
                ", conversationId='" + conversationId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", imageName='" + imageName + '\'' +
                '}';
    }
}