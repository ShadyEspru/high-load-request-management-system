# Request Lifecycle

## Purpose

Defines the official lifecycle of a request inside HLRMS.

## States

| State | Description | Final State |
|---|---|---|
| RECEIVED | The request was received and assigned an identifier. | No |
| VALIDATING | The request is being validated. | No |
| ACCEPTED | The request passed validation. | No |
| REJECTED | The request failed validation. | Yes |
| QUEUED | The request was published to the message queue. | No |
| PROCESSING | A worker is currently processing the request. | No |
| SUCCEEDED | The request completed successfully. | Yes |
| FAILED | The current processing attempt failed. | No |
| RETRY_SCHEDULED | A retry has been scheduled. | No |
| DEAD_LETTERED | Retry attempts were exhausted and the request was moved to the DLQ. | Yes |
| CANCELLED | The request was cancelled. | Yes |

## Normal Flow

RECEIVED → VALIDATING → ACCEPTED → QUEUED → PROCESSING → SUCCEEDED

## Validation Failure

RECEIVED → VALIDATING → REJECTED

## Retry Flow

PROCESSING → FAILED → RETRY_SCHEDULED → QUEUED → PROCESSING

## Permanent Failure

PROCESSING → FAILED → DEAD_LETTERED