# TrustTrail — Blockchain-Based Tourist Review Verification System

TrustTrail is a blockchain-based tourist review verification system designed to reduce fake and unreliable travel reviews.

The system uses visit evidence such as GPS-tagged photographs, image verification, and review analysis to check whether a user has actually visited a location and whether their review appears trustworthy.

Blockchain technology is then used to maintain a tamper-evident record of the review data.

## Problem

Traditional travel platforms allow users to submit reviews without proving that they actually visited the location.

This can lead to:

* Fake reviews
* Spam reviews
* Manipulated ratings
* Reviews from users who never visited the location

TrustTrail addresses this problem by introducing evidence-based review verification.

## How It Works

A user submits a review along with evidence of their visit.

The system then:

1. Extracts location information from the submitted image.
2. Checks whether the location is close to the selected tourist location.
3. Uses image analysis to help verify the visual content.
4. Analyzes the review for quality and possible spam.
5. Combines the verification results to evaluate the trustworthiness of the review.
6. Stores the review data in the application backend.
7. Creates a cryptographic representation of the review data and stores it using an Ethereum smart contract.

## Visit Verification

Users provide a GPS-tagged photograph as evidence that they visited the location.

The system extracts location information from the image and compares it with the selected tourist location.

A geographical distance calculation is used to check whether the submitted evidence is within the required proximity of the claimed location.

## Image Verification

Google Cloud Vision is used to analyze the submitted image.

The image analysis helps determine whether the visual content is relevant to the tourist location provided by the user.

## Review Analysis

The submitted review is analyzed using Stanford CoreNLP.

The system evaluates the review quality and looks for potentially spam-like or low-quality content.

This provides another signal when determining the overall trustworthiness of a review.

## Blockchain Storage

The complete review is handled by the application backend, while a cryptographic representation of the review is stored through an Ethereum smart contract.

This creates a tamper-evident record that can be used to check whether the stored review data has been modified.

The project uses the Ethereum Sepolia test network.

## Trust Evaluation

TrustTrail combines multiple verification signals, including:

* Physical visit evidence
* GPS/location proximity
* Image verification
* Review quality
* Spam detection

These signals are used together to evaluate the trustworthiness of a submitted review.

## Authentication

The backend uses JWT-based authentication to secure user requests and protect application endpoints.

## Technology Stack

### Frontend

* React
* JavaScript

### Backend

* Java
* Spring Boot
* JWT
* Web3j

### Database

* MySQL

### Blockchain

* Solidity
* Ethereum
* Sepolia Testnet
* Web3j

### Verification and Analysis

* Google Cloud Vision API
* Stanford CoreNLP
* EXIF metadata
* Haversine distance calculation
* Nominatim

## Key Features

* Evidence-based tourist review submission
* GPS/EXIF location verification
* Location proximity validation
* Image-based location verification
* NLP-based review analysis
* Spam and low-quality review detection
* JWT authentication
* Blockchain-backed review integrity
* Ethereum smart contract integration
* React-based user interface

## Project Structure

```text
TrustTrail/
├── frontend/
│   └── src/
├── backend/
│   └── verificationApp/
|── ReviewStorage.sol
└── README.md
```

## Future Improvements

* Stronger on-chain verification of review hashes
* Improved image and landmark verification
* More advanced spam detection
* Additional travel platforms and location sources
* Improved decentralized verification mechanisms

## Project

TrustTrail was developed as a project combining full-stack development, image and text analysis, location verification, and blockchain technology.
