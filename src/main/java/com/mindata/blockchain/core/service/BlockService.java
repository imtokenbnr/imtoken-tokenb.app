package com.mindata.blockchain.core.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.mindata.blockchain.block.Block;
import com.mindata.blockchain.block.BlockBody;
import com.mindata.blockchain.block.BlockHeader;
import com.mindata.blockchain.block.Instruction;
import com.mindata.blockchain.block.merkle.MerkleTree;
import com.mindata.blockchain.common.CommonUtil;
import com.mindata.blockchain.common.Sha256;
import com.mindata.blockchain.common.exception.TrustSDKException;
import com.mindata.blockchain.core.requestbody.BlockRequestBody;
import com.mindata.blockchain.socket.client.BlockClientStarter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wuweifeng wrote on 2018/3/8.
 */
@Service
public class BlockService {
    @Resource
    private InstructionService instructionService;
    @Value("${version}")
    private int version;

    /**
     * 校验指令集是否合法
     *
     * @param blockRequestBody
     *         指令集
     * @return 是否合法，为null则校验通过，其他则失败并返回原因
     */
    public String check(BlockRequestBody blockRequestBody) throws TrustSDKException {
        //TODO 此处可能需要校验publicKey的合法性
        if (blockRequestBody == null || blockRequestBody.getBlockBody() == null || StrUtil.isEmpty(blockRequestBody
                .getPublicKey())) {
            return "请求参数缺失";
        }
        List<Instruction> instructions = blockRequestBody.getBlockBody().getTransactions();
        if (CollectionUtil.isEmpty(instructions)) {
            return "指令信息不能为空";
        }

        for (Instruction instruction : instructions) {
            if (!StrUtil.equals(blockRequestBody.getPublicKey(), instruction.getPublicKey())) {
                return "指令内公钥和传来的公钥不匹配";
            }
            if (!instructionService.checkSign(instruction)) {
                return "签名校验不通过";
            }
            if (!instructionService.checkHash(instruction)) {
                return "Hash校验不通过";
            }
        }

        return null;
    }


    public Block addBlock(BlockRequestBody blockRequestBody) {
        Block block = new Block();
        BlockHeader blockHeader = new BlockHeader();
        BlockBody blockBody = blockRequestBody.getBlockBody();
        List<Instruction> instructions = blockBody.getTransactions();
        List<String> hashList = instructions.stream().map(Instruction::getHash).collect(Collectors
                .toList());
        blockHeader.setHashList(hashList);
        //计算所以指令的hashRoot
        blockHeader.setHashMerkleRoot(new MerkleTree(hashList).getRoot());
        blockHeader.setPublicKey(blockRequestBody.getPublicKey());
        blockHeader.setTimeStamp(CommonUtil.getNow());
        blockHeader.setVersion(version);
        block.setBlockBody(blockBody);
        block.setHash(Sha256.sha256(blockHeader.toString() + blockBody.toString()));

        //TODO 广播给其他人做验证

        return block;
    }

}