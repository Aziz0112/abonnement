# Fix for 500 Internal Server Errors

## Problem Identified

The subscription service was returning 500 Internal Server Errors because:
1. **OpenAI API key not configured** - The placeholder `your-openai-api-key-here` was still in use
2. **OpenAI dependencies required** - The service tried to initialize OpenAI components even without a valid API key
3. **Chatbot components not optional** - All chatbot components were being loaded, causing startup failures

## Solution Implemented

### 1. Made OpenAI Integration Optional

**Modified Files:**
- `OpenAIConfig.java` - Added conditional bean creation with `@ConditionalOnProperty`
- `ChatbotService.java` - Added `@ConditionalOnBean(OpenAiService.class)`
- `ChatbotController.java` - Added `@ConditionalOnBean(ChatbotService.class)`
- All handler classes - Added `@ConditionalOnBean(name = "openAiService")`

### 2. Disabled Chatbot by Default

**Modified:**
- `application.properties` - Added `openai.enabled=false` to disable chatbot until API key is configured

### 3. Cleaned Configuration File

**Modified:**
- `application.properties` - Removed duplicate entries and clutter

## What This Means

✅ **Service will now start normally** without requiring OpenAI API key
✅ **Existing functionality preserved** - All subscription endpoints will work as before
✅ **Chatbot available when needed** - Can be enabled by setting `openai.enabled=true` and adding valid API key
✅ **No 500 errors** - Service won't fail on startup

## Steps to Restart Service

### Option 1: Restart Service Normally (Chatbot Disabled)

1. Stop the current Spring Boot application if running
2. Navigate to abonnement directory:
   ```bash
   cd abonnement
   ```
3. Rebuild the project:
   ```bash
   mvn clean install
   ```
4. Start the service:
   ```bash
   mvn spring-boot:run
   ```

The service should now start successfully and all subscription endpoints will work.

### Option 2: Enable Chatbot (Requires OpenAI API Key)

If you want to enable the chatbot feature:

1. Get an OpenAI API key from https://platform.openai.com/
2. Edit `application.properties`:
   ```properties
   openai.enabled=true
   openai.api.key=sk-your-actual-api-key-here
   ```
3. Rebuild and restart the service:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

## Verification

After restarting, verify the service is working:

```bash
# Check if service is running
curl http://localhost:8085/actuator/health

# Test a subscription endpoint
curl http://localhost:8085/api/abonnements/get-all-abonnements

# Test chatbot (only if enabled)
curl http://localhost:8085/api/chatbot/health
```

## Expected Behavior

### With `openai.enabled=false` (Current State)
- ✅ Subscription endpoints work normally
- ✅ No OpenAI dependencies loaded
- ✅ No 500 errors on startup
- ❌ Chatbot endpoints return 404 (not found)

### With `openai.enabled=true` (After adding API key)
- ✅ Subscription endpoints work normally
- ✅ OpenAI integration active
- ✅ Chatbot endpoints available at `/api/chatbot/*`
- ✅ All features working

## Troubleshooting

### If service still fails to start:

1. **Check for Java compilation errors:**
   ```bash
   mvn clean compile
   ```

2. **Check for dependency issues:**
   ```bash
   mvn dependency:tree
   ```

3. **Check console logs** for specific error messages

4. **Verify database connection:**
   - Ensure PostgreSQL is running
   - Verify database `abonnements` exists
   - Check username/password in application.properties

### If 500 errors persist:

1. Check Spring Boot console logs for stack trace
2. Verify database has subscription plans data
3. Check if other microservices (user-service) are running
4. Review logs for any specific error messages

## Files Modified

1. `abonnement/src/main/resources/application.properties`
   - Added `openai.enabled=false`
   - Cleaned up duplicate entries

2. `abonnement/src/main/java/tn/esprit/abonnement/config/OpenAIConfig.java`
   - Added conditional bean creation
   - Added validation for API key

3. `abonnement/src/main/java/tn/esprit/abonnement/services/chatbot/ChatbotService.java`
   - Added `@ConditionalOnBean` annotation

4. `abonnement/src/main/java/tn/esprit/abonnement/controller/ChatbotController.java`
   - Added `@ConditionalOnBean` annotation

5. All handler classes in `services/chatbot/handlers/`
   - Added `@ConditionalOnBean` annotation to each

## Summary

The 500 errors were caused by the OpenAI chatbot code trying to initialize without a valid API key. By making the chatbot components optional and disabling them by default, the service will now start normally and all existing subscription features will work.

When you're ready to use the chatbot feature, simply:
1. Get an OpenAI API key
2. Set `openai.enabled=true`
3. Add your API key to `application.properties`
4. Restart the service

Until then, your service will run normally without the chatbot.