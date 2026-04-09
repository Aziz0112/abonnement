# 🔌 Remote Database Setup Guide

## Overview
This guide explains how to configure the churn predictor to connect to a PostgreSQL database hosted on a remote server.

---

## 📋 Prerequisites

- Remote PostgreSQL server access
- Database credentials (host, port, username, password, database name)
- Network connectivity to the server
- VPN or SSH access (if required by your organization)

---

## 🔧 Configuration Options

### Option 1: Direct Connection (Recommended)

Use this if the database server is accessible directly from your machine.

#### Step 1: Create .env File

```bash
cd abonnement/churn-predictor
cp .env.example .env
```

#### Step 2: Configure Remote Database

Edit `.env` file:

```bash
# Database Configuration
DB_HOST=your-database-server.com  # or IP address like 192.168.1.100
DB_PORT=5432
DB_NAME=abonnements
DB_USER=your_database_user
DB_PASSWORD=your_secure_password

# API Configuration
API_HOST=0.0.0.0
API_PORT=5000
API_DEBUG=False  # Set to False in production
```

**Important Security Notes:**
- Never commit `.env` to version control
- Use strong passwords
- Consider using environment variables instead of plain text
- Use SSL/TLS connections if available

#### Step 3: Test Connection

```bash
python -c "
from sqlalchemy import create_engine
import os
from dotenv import load_dotenv

load_dotenv()
DATABASE_URL = f\"postgresql://{os.getenv('DB_USER')}:{os.getenv('DB_PASSWORD')}@{os.getenv('DB_HOST')}:{os.getenv('DB_PORT')}/{os.getenv('DB_NAME')}\"

try:
    engine = create_engine(DATABASE_URL, connect_args={'connect_timeout': 10})
    with engine.connect() as conn:
        print('✓ Database connection successful!')
except Exception as e:
    print(f'✗ Connection failed: {e}')
"
```

---

### Option 2: SSH Tunnel (For Secure Access)

Use this if direct access is blocked but SSH access is available.

#### Step 1: Create SSH Tunnel

```bash
# From your local machine
ssh -L 5433:localhost:5432 your_username@your-server.com

# This forwards local port 5433 to remote port 5432
```

#### Step 2: Configure .env for Local Tunnel

```bash
DB_HOST=localhost
DB_PORT=5433  # Local tunnel port
DB_NAME=abonnements
DB_USER=your_database_user
DB_PASSWORD=your_secure_password
```

#### Step 3: Keep Tunnel Alive

Use SSH keep-alive to prevent disconnections:

```bash
ssh -o ServerAliveInterval=60 -o ServerAliveCountMax=5 -L 5433:localhost:5432 your_username@your-server.com
```

#### Step 4: Auto-Start Tunnel (Optional)

Create a systemd service or use autossh:

```bash
# Install autossh
sudo apt install autossh  # Ubuntu/Debian
brew install autossh        # macOS

# Start autossh tunnel
autossh -M 0 -o "ServerAliveInterval 60" -o "ServerAliveCountMax 3" -L 5433:localhost:5432 your_username@your-server.com -N
```

---

### Option 3: VPN Connection

Use this if database is on a private network.

#### Step 1: Connect to VPN

```bash
# Example: Connect to your organization's VPN
sudo openvpn --config company-vpn.ovpn
```

#### Step 2: Configure .env with Private IP

```bash
DB_HOST=192.168.10.50  # Private network IP
DB_PORT=5432
DB_NAME=abonnements
DB_USER=your_database_user
DB_PASSWORD=your_secure_password
```

#### Step 3: Test Connectivity

```bash
ping 192.168.10.50
telnet 192.168.10.50 5432
```

---

## 🔒 Security Best Practices

### 1. Use Environment Variables

Instead of hardcoding passwords, use environment variables:

```python
# config.py
import os
from dotenv import load_dotenv

load_dotenv()

DB_PASSWORD = os.getenv("DB_PASSWORD")
```

Set environment variable in shell:
```bash
export DB_PASSWORD="your_secure_password"
```

### 2. Use SSL/TLS Connection

Configure SQLAlchemy to use SSL:

```python
# database_utils.py
DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}?sslmode=require"

engine = create_engine(
    DATABASE_URL,
    connect_args={
        'sslmode': 'require',
        'sslrootcert': '/path/to/ca-certificate.crt'
    }
)
```

### 3. Restrict Database Access

On the PostgreSQL server, configure `pg_hba.conf`:

```bash
# Allow only specific IP addresses
host    abonnements    your_username    192.168.1.0/24    scram-sha-256

# Or require SSL
hostssl abonnements    your_username    0.0.0.0/0    scram-sha-256
```

### 4. Use Read-Only User for Training

Create a separate database user with limited permissions:

```sql
-- On the PostgreSQL server
CREATE USER churn_predictor WITH PASSWORD 'read_only_password';
GRANT CONNECT ON DATABASE abonnements TO churn_predictor;
GRANT USAGE ON SCHEMA public TO churn_predictor;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO churn_predictor;
```

Configure `.env`:
```bash
DB_USER=churn_predictor
DB_PASSWORD=read_only_password
```

---

## 🚀 Production Deployment

### Deploy on Same Server as Database

If deploying the churn predictor on the same server as the database:

#### Step 1: Upload Files

```bash
# From your local machine
scp -r churn-predictor/ your_username@your-server.com:/path/to/deploy/
```

#### Step 2: Configure .env on Server

```bash
ssh your_username@your-server.com
cd /path/to/deploy/churn-predictor
nano .env
```

Edit `.env`:
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=abonnements
DB_USER=your_database_user
DB_PASSWORD=your_secure_password
API_HOST=0.0.0.0
API_PORT=5000
API_DEBUG=False
```

#### Step 3: Install Dependencies

```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

#### Step 4: Train Models

```bash
python train_model.py
```

#### Step 5: Start API Service

```bash
# Create systemd service file
sudo nano /etc/systemd/system/churn-predictor.service
```

Add content:
```ini
[Unit]
Description=Churn Prediction API
After=network.target

[Service]
Type=simple
User=your_username
WorkingDirectory=/path/to/deploy/churn-predictor
Environment="PATH=/path/to/deploy/churn-predictor/venv/bin"
ExecStart=/path/to/deploy/churn-predictor/venv/bin/python app.py
Restart=always

[Install]
WantedBy=multi-user.target
```

Start service:
```bash
sudo systemctl daemon-reload
sudo systemctl start churn-predictor
sudo systemctl enable churn-predictor
sudo systemctl status churn-predictor
```

---

## 🐛 Troubleshooting

### Issue: Connection Refused

**Error**: `psycopg2.OperationalError: could not connect to server`

**Solutions**:
1. Check if database is running: `sudo systemctl status postgresql`
2. Check firewall: `sudo ufw status` or `sudo iptables -L`
3. Verify host and port: `telnet your-database-server.com 5432`
4. Check if VPN/SSH is required

### Issue: Connection Timeout

**Error**: `psycopg2.OperationalError: timeout expired`

**Solutions**:
1. Check network connectivity: `ping your-database-server.com`
2. Increase timeout in SQLAlchemy:
```python
engine = create_engine(DATABASE_URL, connect_args={'connect_timeout': 30})
```
3. Check if server is blocking connections (firewall/security groups)

### Issue: Authentication Failed

**Error**: `psycopg2.OperationalError: FATAL: password authentication failed`

**Solutions**:
1. Verify username and password in `.env`
2. Check if user exists: `\du` in PostgreSQL
3. Check if database exists: `\l` in PostgreSQL
4. Verify password hasn't expired

### Issue: SSL Required

**Error**: `psycopg2.OperationalError: FATAL: no pg_hba.conf entry for host`

**Solutions**:
1. Add SSL to connection string: `?sslmode=require`
2. Configure SSL certificates if needed
3. Check `pg_hba.conf` on server

### Issue: Permission Denied

**Error**: `psycopg2.errors.InsufficientPrivilege`

**Solutions**:
1. Grant SELECT permissions to user:
```sql
GRANT SELECT ON ALL TABLES IN SCHEMA public TO your_username;
```
2. Check user permissions: `\dp` in PostgreSQL
3. Use superuser if needed (not recommended for production)

---

## 📊 Testing Database Queries

Before training models, test data fetching:

```python
# test_db_connection.py
from database_utils import fetch_real_data, create_churn_predictions_table

print("Testing database connection...")
print("Creating churn_predictions table...")
create_churn_predictions_table()

print("\nFetching real data...")
df = fetch_real_data()

if df is not None:
    print(f"✓ Successfully fetched {len(df)} records")
    print(f"\nColumns: {list(df.columns)}")
    print(f"\nFirst few rows:")
    print(df.head())
else:
    print("✗ Failed to fetch data, using synthetic")
```

Run test:
```bash
python test_db_connection.py
```

---

## 🔄 Updating Configuration

### Changing Database Server

If you need to switch database servers:

1. Stop the API:
```bash
# If running as service
sudo systemctl stop churn-predictor
```

2. Update `.env`:
```bash
nano .env
# Change DB_HOST, DB_PORT, DB_USER, DB_PASSWORD
```

3. Test connection:
```bash
python -c "from database_utils import fetch_real_data; print('Connected!' if fetch_real_data() is not None else 'Failed')"
```

4. Retrain models (optional but recommended):
```bash
python train_model.py
```

5. Start API:
```bash
sudo systemctl start churn-predictor
```

---

## 📝 Example Configurations

### Development (Local Database)
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=abonnements
DB_USER=postgres
DB_PASSWORD=dev_password
API_DEBUG=True
```

### Staging (Remote Server via VPN)
```bash
DB_HOST=192.168.10.50
DB_PORT=5432
DB_NAME=abonnements_staging
DB_USER=staging_user
DB_PASSWORD=staging_password
API_DEBUG=True
```

### Production (Remote Server with SSL)
```bash
DB_HOST=production-db.yourcompany.com
DB_PORT=5432
DB_NAME=abonnements
DB_USER=churn_predictor
DB_PASSWORD=secure_production_password
API_DEBUG=False
```

---

## 🎯 Quick Start Checklist

- [ ] Database server is accessible from your machine
- [ ] Have database credentials (host, port, username, password, database name)
- [ ] Created `.env` file with correct configuration
- [ ] Tested database connection successfully
- [ ] Installed all Python dependencies
- [ ] Trained models with `python train_model.py`
- [ ] Started API with `python app.py`
- [ ] Tested API endpoint: `curl http://localhost:5000/health`
- [ ] Configured production deployment (if applicable)
- [ ] Set up monitoring and logging

---

## 📞 Support

If you encounter issues:

1. Check PostgreSQL server logs: `tail -f /var/log/postgresql/postgresql-*.log`
2. Check API logs for connection errors
3. Verify network connectivity with `telnet` or `nc`
4. Review PostgreSQL configuration: `/etc/postgresql/*/main/pg_hba.conf`
5. Contact your database administrator if needed

---

## 🔗 Useful Commands

### PostgreSQL
```bash
# Connect to database
psql -h your-server.com -U your_username -d abonnements

# List tables
\dt

# Describe table
\d usersubscriptions

# Run query
SELECT COUNT(*) FROM usersubscriptions;

# Exit
\q
```

### Network
```bash
# Test port connectivity
telnet your-server.com 5432
nc -zv your-server.com 5432

# Ping server
ping your-server.com

# Trace route
traceroute your-server.com
```

### Python
```bash
# Test connection
python -c "import psycopg2; conn = psycopg2.connect(host='your-server.com', database='abonnements', user='your_username', password='your_password'); print('Connected!')"

# Check installed packages
pip list | grep -E "(psycopg|sqlalchemy)"
```

---

## 📚 Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [SQLAlchemy Documentation](https://docs.sqlalchemy.org/)
- [psycopg2 Documentation](https://www.psycopg.org/docs/)
- [SSH Tunneling Guide](https://help.github.com/articles/using-ssh-over-the-https-port/)

---

**Last Updated**: April 10, 2026