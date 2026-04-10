# Subscription Support Chatbot - Implementation Guide

## Overview

This document describes the OpenAI-powered chatbot implementation for the subscription management service. The chatbot helps users understand subscription plans, get personalized recommendations, resolve payment issues, and manage their subscriptions.

## Features

✅ **Plan Information** - Explains all available subscription plans in simple terms  
✅ **Personalized Recommendations** - Suggests the best plan based on user usage  
✅ **Payment Support** - Diagnoses and resolves payment issues  
✅ **Subscription Management** - Guides users through upgrade, downgrade, and cancellation  
✅ **Context-Aware** - Uses real subscription data to personalize responses  
✅ **Friendly & Simple** - Avoids technical jargon and keeps responses clear

## Architecture

### Components

1. **ChatbotController** - REST API endpoint for chatbot messages
2. **ChatbotService** - Core service handling intent classification and routing
3. **ChatbotDataFetcher** - Retrieves user subscription data
4. **Intent Handlers** - Specialized handlers for different user intents:
   - `PlanInfoHandler` - Explains plans and features
   - `PlanRecommendationHandler` - Provides personalized recommendations
   - `PaymentIssueHandler` - Helps resolve payment problems
   - `SubscriptionManagementHandler` - Guides subscription changes

### API Endpoint

```
POST /api/chatbot/message
```

**Request:**
```json
{
  "userId": 123,
  "message": "Which plan should I choose?",
  "conversationContext": {}
}
```

**Response:**
```json
{
  "response": "You are close to your monthly limit.\n\n👉 The PREMIUM plan would be a better fit for your usage.",
  "suggestedActions": ["Upgrade Now", "View Plans", "Learn More"],
  "recommendedPlan": "PREMIUM",
  "intent": "PLAN_RECOMMENDATION"
}
```

## Setup Instructions

### 1. Add OpenAI API Key

Edit `abonnement/src/main/resources/application.properties`:

```properties
openai.api.key=sk-your-actual-openai-api-key-here
openai.model=gpt-4o-mini
```

**To get an OpenAI API key:**
1. Go to https://platform.openai.com/
2. Sign up or log in
3. Navigate to API Keys section
4. Create a new API key
5. Copy and paste it into the configuration

### 2. Install Dependencies

The OpenAI dependency has been added to `pom.xml`. Run:

```bash
cd abonnement
mvn clean install
```

### 3. Start the Service

```bash
mvn spring-boot:run
```

The chatbot will be available at `http://localhost:8085/api/chatbot/message`

## Usage Examples

### Example 1: Plan Information

**User:** "Tell me about your subscription plans"

**Response:**
```
Here are our available subscription plans:

🎁 **FREEMIUM** - FREE
  Perfect for getting started

⭐ **STANDARD** - $19.99/month
  Great for regular users

🚀 **PREMIUM** - $49.99/month
  For power users and businesses

Which plan would you like to know more about?
```

### Example 2: Plan Recommendation

**User:** "Which plan should I choose?"

**Response:**
```
You are close to your monthly limit.

👉 The PREMIUM plan would be a better fit for your usage.

Upgrade now to get unlimited access and priority support!
```

### Example 3: Payment Issue

**User:** "Why did my payment fail?"

**Response:**
```
Your payment may have failed due to:

• Expired card
• Insufficient funds
• Bank blocking the transaction
• Incorrect card details

👉 Please check your card details or try another payment method.

Next steps:
1. Go to Settings → Billing
2. Update your payment method
3. Try the payment again
```

### Example 4: How to Upgrade

**User:** "How do I upgrade?"

**Response:**
```
You can upgrade your plan from your dashboard:

👉 Go to Settings → Subscription → Upgrade Plan

The change will be applied instantly.

Your new features will be available immediately after the upgrade!
```

## Intent Categories

The chatbot classifies user messages into the following intents:

- **PLAN_INFO** - User asks about plans, pricing, features
- **PLAN_RECOMMENDATION** - User asks which plan to choose
- **PAYMENT_ISSUE** - User mentions payment failure, billing problems
- **UPGRADE** - User asks how to upgrade
- **DOWNGRADE** - User asks how to downgrade
- **CANCEL** - User wants to cancel
- **MANAGE_SUBSCRIPTION** - User wants to view or manage their subscription
- **GENERAL** - General questions or greetings

## Behavior Rules

The chatbot follows these rules:

1. **Always be short and clear** - Keep responses concise
2. **Be helpful and friendly** - Use a welcoming tone
3. **Avoid technical jargon** - Use simple language
4. **Personalize responses** - Use actual user subscription data
5. **Ask clarifying questions** - When unsure about the user's needs

## Testing the Chatbot

### Using cURL

```bash
curl -X POST http://localhost:8085/api/chatbot/message \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "message": "Which plan should I choose?",
    "conversationContext": {}
  }'
```

### Using Postman

1. Create a new POST request
2. URL: `http://localhost:8085/api/chatbot/message`
3. Headers: `Content-Type: application/json`
4. Body (raw JSON):
```json
{
  "userId": 1,
  "message": "What plans do you have?",
  "conversationContext": {}
}
```

### Health Check

```bash
curl http://localhost:8085/api/chatbot/health
```

Expected response: `Chatbot service is running`

## Integration with Frontend

### JavaScript Example

```javascript
async function sendMessage(userId, message) {
  const response = await fetch('http://localhost:8085/api/chatbot/message', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      userId: userId,
      message: message,
      conversationContext: {}
    })
  });

  const data = await response.json();
  
  // Display the response
  console.log(data.response);
  
  // Show suggested actions as buttons
  if (data.suggestedActions && data.suggestedActions.length > 0) {
    data.suggestedActions.forEach(action => {
      console.log('Action:', action);
    });
  }
  
  // Handle recommended plan
  if (data.recommendedPlan) {
    console.log('Recommended Plan:', data.recommendedPlan);
  }
}
```

### React Component Example

```jsx
import React, { useState } from 'react';

function Chatbot({ userId }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');

  const sendMessage = async () => {
    const response = await fetch('http://localhost:8085/api/chatbot/message', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId,
        message: input,
        conversationContext: {}
      })
    });
    
    const data = await response.json();
    setMessages([...messages, 
      { type: 'user', text: input },
      { type: 'bot', text: data.response, actions: data.suggestedActions }
    ]);
    setInput('');
  };

  return (
    <div className="chatbot">
      <div className="messages">
        {messages.map((msg, i) => (
          <div key={i} className={`message ${msg.type}`}>
            <p>{msg.text}</p>
            {msg.actions && (
              <div className="actions">
                {msg.actions.map((action, j) => (
                  <button key={j}>{action}</button>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
      <div className="input">
        <input 
          value={input} 
          onChange={(e) => setInput(e.target.value)} 
          placeholder="Ask about subscriptions..."
        />
        <button onClick={sendMessage}>Send</button>
      </div>
    </div>
  );
}
```

## Cost Optimization

The chatbot uses **GPT-4o-mini** which is:
- **Fast** - Quick response times
- **Cost-effective** - Lower API costs
- **Accurate** - Good intent classification

To further optimize costs:
1. Cache common responses
2. Use shorter system prompts
3. Limit response tokens (currently set to 200 for general responses)

## Error Handling

The chatbot includes robust error handling:
- Missing userId: Returns 400 Bad Request
- Missing message: Returns 400 Bad Request
- API errors: Returns 500 Internal Server Error with logging
- Intent classification failures: Defaults to GENERAL intent

## Logs

The chatbot logs important events:
- Message received: `INFO - Received chatbot message from user: {userId}`
- Response generated: `INFO - Chatbot response generated successfully for user: {userId}`
- Errors: `ERROR - Error processing chatbot message for user {userId}: {error}`

## Troubleshooting

### Issue: "Chatbot request missing userId"

**Solution:** Ensure the request body includes a valid `userId` field.

### Issue: "Chatbot request missing message"

**Solution:** Ensure the request body includes a non-empty `message` field.

### Issue: OpenAI API errors

**Solution:** 
1. Verify your API key is correct in `application.properties`
2. Check your OpenAI account has sufficient credits
3. Ensure you have internet connectivity

### Issue: Repository method not found

**Solution:** Ensure `UserSubscriptionRepository` has the `findActiveSubscriptionByUserId` method.

## Future Enhancements

Potential improvements:
1. **Conversation History** - Store and maintain context across multiple messages
2. **Multilingual Support** - Support multiple languages
3. **Usage Analytics** - Track common questions and improve responses
4. **Custom Training** - Fine-tune the model on subscription-specific data
5. **Voice Support** - Add voice-to-text and text-to-voice capabilities

## Support

For issues or questions:
- Check the logs in the console
- Verify the OpenAI API key is set correctly
- Ensure the database has subscription plan data
- Test the API endpoint using cURL or Postman

## Files Created

- `ChatbotController.java` - REST API controller
- `ChatbotService.java` - Core chatbot logic
- `ChatbotDataFetcher.java` - Data retrieval service
- `OpenAIConfig.java` - OpenAI configuration
- `PlanInfoHandler.java` - Plan information handler
- `PlanRecommendationHandler.java` - Recommendation handler
- `PaymentIssueHandler.java` - Payment support handler
- `SubscriptionManagementHandler.java` - Subscription management handler
- `ChatbotRequest.java` - Request DTO
- `ChatbotResponse.java` - Response DTO
- `SubscriptionSummary.java` - Subscription summary DTO

## Modified Files

- `pom.xml` - Added OpenAI Java SDK dependency
- `application.properties` - Added OpenAI configuration