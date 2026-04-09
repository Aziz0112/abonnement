"""
Test database connection script for remote PostgreSQL server.
Run this to verify your database configuration before training models.
"""

import sys
from sqlalchemy import create_engine, text
import os
from dotenv import load_dotenv

def test_connection():
    """Test database connection and fetch basic information."""
    print("=" * 60)
    print("DATABASE CONNECTION TEST")
    print("=" * 60)
    
    # Load environment variables
    load_dotenv()
    
    DB_HOST = os.getenv("DB_HOST", "localhost")
    DB_PORT = os.getenv("DB_PORT", "5432")
    DB_NAME = os.getenv("DB_NAME", "abonnements")
    DB_USER = os.getenv("DB_USER", "postgres")
    DB_PASSWORD = os.getenv("DB_PASSWORD", "")
    
    print(f"\nConfiguration:")
    print(f"  Host: {DB_HOST}")
    print(f"  Port: {DB_PORT}")
    print(f"  Database: {DB_NAME}")
    print(f"  User: {DB_USER}")
    print(f"  Password: {'*' * len(DB_PASSWORD)}")
    
    # Create connection URL
    DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
    
    print(f"\nAttempting to connect...")
    
    try:
        # Test basic connection
        engine = create_engine(DATABASE_URL, connect_args={'connect_timeout': 10})
        
        with engine.connect() as conn:
            print("✓ Connection successful!")
            
            # Test database version
            result = conn.execute(text("SELECT version()"))
            version = result.fetchone()[0]
            print(f"\nDatabase Version:")
            print(f"  {version.split(',')[0]}")
            
            # Test if required tables exist
            print(f"\nChecking tables...")
            tables_query = """
                SELECT table_name 
                FROM information_schema.tables 
                WHERE table_schema = 'public' 
                ORDER BY table_name
            """
            result = conn.execute(text(tables_query))
            tables = [row[0] for row in result]
            
            required_tables = ['usersubscriptions', 'subscriptionplans', 'invoices']
            found_tables = [t for t in required_tables if t.lower() in [t2.lower() for t2 in tables]]
            
            print(f"  Total tables found: {len(tables)}")
            print(f"  Required tables:")
            for table in required_tables:
                status = "✓" if table.lower() in [t.lower() for t in tables] else "✗"
                print(f"    {status} {table}")
            
            if len(found_tables) < len(required_tables):
                print(f"\n⚠ Warning: Not all required tables found!")
                print(f"   Missing: {set(required_tables) - set([t.lower() for t in found_tables])}")
            else:
                print(f"\n✓ All required tables found!")
            
            # Test fetching sample data
            if 'usersubscriptions' in [t.lower() for t in tables]:
                print(f"\nTesting data fetch from usersubscriptions...")
                count_query = "SELECT COUNT(*) FROM usersubscriptions"
                result = conn.execute(text(count_query))
                count = result.fetchone()[0]
                print(f"  Total subscriptions: {count}")
                
                if count > 0:
                    sample_query = """
                        SELECT us.id, us.user_id, us.status, sp.name as plan_name
                        FROM usersubscriptions us
                        LEFT JOIN subscriptionplans sp ON us.plan_id = sp.id
                        LIMIT 3
                    """
                    result = conn.execute(text(sample_query))
                    samples = result.fetchall()
                    print(f"  Sample records:")
                    for sample in samples:
                        print(f"    ID: {sample[0]}, User: {sample[1]}, Status: {sample[2]}, Plan: {sample[3]}")
                else:
                    print(f"  ⚠ Warning: No subscription data found")
            
            # Test if churn_predictions table exists
            if 'churn_predictions' in [t.lower() for t in tables]:
                print(f"\n✓ churn_predictions table exists")
                count_query = "SELECT COUNT(*) FROM churn_predictions"
                result = conn.execute(text(count_query))
                count = result.fetchone()[0]
                print(f"  Existing predictions: {count}")
            else:
                print(f"\n⚠ churn_predictions table does not exist (will be created on training)")
            
        print(f"\n" + "=" * 60)
        print("✓ ALL TESTS PASSED!")
        print("=" * 60)
        print(f"\nYou can now train models with: python train_model.py")
        print(f"Or start the API with: python app.py")
        return True
        
    except Exception as e:
        print(f"\n✗ Connection failed: {e}")
        print(f"\n" + "=" * 60)
        print("TROUBLESHOOTING")
        print("=" * 60)
        
        error_str = str(e).lower()
        
        if "connection refused" in error_str or "could not connect" in error_str:
            print("\n1. Connection Refused")
            print("   Possible causes:")
            print("   - PostgreSQL is not running on the server")
            print("   - Wrong host or port")
            print("   - Firewall is blocking the connection")
            print("   - Server is not accessible from your network")
            print("\n   Solutions:")
            print("   - Verify PostgreSQL is running: sudo systemctl status postgresql")
            print("   - Check host and port: telnet {} {}".format(DB_HOST, DB_PORT))
            print("   - Check firewall rules on the server")
            print("   - Verify you're connected to VPN if required")
        
        elif "timeout expired" in error_str:
            print("\n2. Connection Timeout")
            print("   Possible causes:")
            print("   - Network connectivity issues")
            print("   - Server is slow to respond")
            print("   - Firewall is blocking but not rejecting")
            print("\n   Solutions:")
            print("   - Ping the server: ping {}".format(DB_HOST))
            print("   - Check network connectivity")
            print("   - Increase timeout in database_utils.py")
        
        elif "password authentication failed" in error_str or "fatal: password" in error_str:
            print("\n3. Authentication Failed")
            print("   Possible causes:")
            print("   - Wrong username or password")
            print("   - User does not exist")
            print("   - Password has expired")
            print("\n   Solutions:")
            print("   - Verify credentials in .env file")
            print("   - Check if user exists in PostgreSQL: \\du")
            print("   - Reset password if needed")
        
        elif "database" in error_str and "does not exist" in error_str:
            print("\n4. Database Does Not Exist")
            print("   Possible causes:")
            print("   - Database name is incorrect")
            print("   - Database hasn't been created")
            print("\n   Solutions:")
            print("   - Verify database name in .env file")
            print("   - Create database: CREATE DATABASE {};".format(DB_NAME))
        
        elif "permission denied" in error_str or "insufficient privilege" in error_str:
            print("\n5. Permission Denied")
            print("   Possible causes:")
            print("   - User doesn't have access to database")
            print("   - User doesn't have SELECT permissions")
            print("\n   Solutions:")
            print("   - Grant permissions: GRANT SELECT ON ALL TABLES TO {};".format(DB_USER))
            print("   - Use a user with appropriate permissions")
        
        elif "ssl" in error_str:
            print("\n6. SSL Required")
            print("   Possible causes:")
            print("   - Server requires SSL connection")
            print("   - SSL certificate issues")
            print("\n   Solutions:")
            print("   - Add ?sslmode=require to DATABASE_URL")
            print("   - Configure SSL certificates")
            print("   - Check server SSL configuration")
        
        else:
            print("\nUnknown Error")
            print("   Please check:")
            print("   - PostgreSQL server logs")
            print("   - Network connectivity")
            print("   - Server configuration")
            print("\n   For more help, see: REMOTE_DATABASE_SETUP.md")
        
        print(f"\n" + "=" * 60)
        return False

if __name__ == "__main__":
    success = test_connection()
    sys.exit(0 if success else 1)