import pytest
from unittest.mock import patch, MagicMock
from fastapi import HTTPException
from app.services.auth_service import AuthService
from app.schemas.auth_schema import FirebaseLoginRequest

# Mock settings
with patch("app.core.config.settings") as mock_settings:
    mock_settings.JWT_SECRET_KEY = "test_secret"
    mock_settings.JWT_ALGORITHM = "HS256"
    mock_settings.JWT_EXPIRES_IN = 3600

@pytest.fixture
def db_session():
    return MagicMock()

@patch("app.services.auth_service.auth.verify_id_token")
@patch("app.services.auth_service.create_access_token")
@patch("app.services.auth_service.create_refresh_token")
def test_authenticate_with_firebase_success(
    mock_refresh, mock_access, mock_verify, db_session
):
    # Setup Mocks
    mock_verify.return_value = {"phone_number": "+1234567890"}
    mock_access.return_value = "access_token"
    mock_refresh.return_value = "refresh_token"
    
    # Setup DB Mock
    mock_student = MagicMock()
    mock_student.student_id = "S123"
    mock_student.id = 1
    mock_student.phone_number = "+1234567890"

    # Configure the query chain: db.query().filter().first()
    db_session.query.return_value.filter.return_value.first.return_value = mock_student

    # Input Data
    login_req = FirebaseLoginRequest(token="valid_token", role="student")

    # Call Service
    response = AuthService.authenticate_with_firebase(db_session, login_req)

    # Assertions
    assert response.access_token == "access_token"
    assert response.refresh_token == "refresh_token"
    
    # Verify DB was queried correctly
    db_session.query.assert_called() 


@patch("app.services.auth_service.auth.verify_id_token")
def test_authenticate_with_firebase_invalid_token(mock_verify, db_session):
    mock_verify.side_effect = Exception("Invalid token")
    
    login_req = FirebaseLoginRequest(token="invalid_token", role="student")

    with pytest.raises(HTTPException) as exc:
        AuthService.authenticate_with_firebase(db_session, login_req)
    
    assert exc.value.status_code == 401


@patch("app.services.auth_service.auth.verify_id_token")
def test_authenticate_with_firebase_user_not_found(mock_verify, db_session):
    mock_verify.return_value = {"phone_number": "+1234567890"}
    
    # Simulate user not found
    db_session.query.return_value.filter.return_value.first.return_value = None

    login_req = FirebaseLoginRequest(token="valid_token", role="student")

    with pytest.raises(HTTPException) as exc:
        AuthService.authenticate_with_firebase(db_session, login_req)
    
    assert exc.value.status_code == 404
