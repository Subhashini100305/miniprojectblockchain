// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract ReviewStorage {

    address public owner;

    struct Review {
        string reviewHash;
        string placeId;
        uint8 aiConfidenceScore;
        uint8 nlpQualityScore;
        bool gpsVerified;
        uint256 timestamp;
        address reviewer;
    }

    mapping(uint => Review) public reviews;
    uint public reviewCount;

    event ReviewStored(
        string reviewHash,
        string placeId,
        uint8 aiConfidenceScore,
        uint8 nlpQualityScore,
        bool gpsVerified,
        address reviewer,
        uint256 timestamp
    );

    constructor() {
        owner = msg.sender;
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can store reviews");
        _;
    }

    function storeReview(
        string memory _reviewHash,
        string memory _placeId,
        uint8 _aiScore,
        uint8 _nlpScore,
        bool _gpsVerified
    ) public onlyOwner {

        reviews[reviewCount] = Review(
            _reviewHash,
            _placeId,
            _aiScore,
            _nlpScore,
            _gpsVerified,
            block.timestamp,
            msg.sender
        );

        emit ReviewStored(
            _reviewHash,
            _placeId,
            _aiScore,
            _nlpScore,
            _gpsVerified,
            msg.sender,
            block.timestamp
        );

        reviewCount++;
    }

    function getReview(uint index) public view returns (
        string memory,
        string memory,
        uint8,
        uint8,
        bool,
        uint256,
        address
    ) {
        Review memory r = reviews[index];
        return (
            r.reviewHash,
            r.placeId,
            r.aiConfidenceScore,
            r.nlpQualityScore,
            r.gpsVerified,
            r.timestamp,
            r.reviewer
        );
    }

    function getTotalReviews() public view returns (uint) {
        return reviewCount;
    }
}
