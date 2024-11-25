//SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract ChatVerificationSystem {
    struct VerificationData {
        bytes32 hashedContent;
        bool isVerified;
        bool exists;
        uint256 timestamp;
        uint256 expireTime;
    }

    mapping(uint256 => mapping(bytes32 => VerificationData)) private verifications;
    address private owner;
    uint256 private constant VERIFICATION_TIMEOUT = 24 hours;
    bytes32 private serverSalt;  // 서버만 아는 salt 값

    event VerificationRequired(uint256 indexed roomId, bytes32 indexed messageId);
    event VerificationSuccessful(uint256 indexed roomId, bytes32 indexed messageId);
    event VerificationFailed(uint256 indexed roomId, bytes32 indexed messageId);

    constructor(bytes32 _serverSalt) {
        owner = msg.sender;
        serverSalt = _serverSalt;
    }

    // 내부적으로 해시를 생성하는 private 함수
    function generateHash(bytes32 content, bytes32 salt) private pure returns (bytes32) {
        return sha256(abi.encodePacked(content, salt));
    }

    // 서버만 호출 가능한 해시 설정 함수
    function setVerificationHash(
        uint256 roomId,
        bytes32 messageId,
        bytes32 hashedContent    // 서버에서 미리 해시한 값
    ) public {
        require(msg.sender == owner, "Only AI server can set verification");

        verifications[roomId][messageId] = VerificationData({
            hashedContent: hashedContent,
            isVerified: false,
            exists: true,
            timestamp: block.timestamp,
            expireTime: block.timestamp + VERIFICATION_TIMEOUT
        });

        emit VerificationRequired(roomId, messageId);
    }

    // 내부적으로 검증을 수행하는 private 함수
    function verifyContent(
        uint256 roomId,
        bytes32 messageId,
        bytes32 input
    ) private returns (bool) {
        VerificationData storage data = verifications[roomId][messageId];

        if (!data.exists || data.isVerified || block.timestamp >= data.expireTime) {
            return false;
        }

        bytes32 hashedInput = generateHash(input, serverSalt);
        if (hashedInput == data.hashedContent) {
            data.isVerified = true;
            emit VerificationSuccessful(roomId, messageId);
            return true;
        }

        emit VerificationFailed(roomId, messageId);
        return false;
    }

    // 외부에서 호출되는 검증 함수
    function verifyMessage(
        uint256 roomId,
        bytes32 messageId,
        bytes32 input
    ) public returns (bool) {
        return verifyContent(roomId, messageId, input);
    }

    // 검증 상태만 조회하는 함수
    function checkVerification(
        uint256 roomId,
        bytes32 messageId
    ) public view returns (
        bool exists,
        bool isVerified,
        bool isExpired
    ) {
        VerificationData storage data = verifications[roomId][messageId];
        return (
            data.exists,
            data.isVerified,
            block.timestamp >= data.expireTime
        );
    }
}