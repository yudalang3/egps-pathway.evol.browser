package module.multiseq.alignment.view;

import module.evoltrepipline.alignment.SequenceDataForAViewer;
import module.multiseq.alignment.DataFileSuffixNames;
import module.multiseq.alignment.view.io.SaveFilterFastaContinuous;
import module.multiseq.alignment.view.io.SaveFilterFastaInterleaved;
import msaoperator.alignment.sequence.BasicSequenceData;
import msaoperator.alignment.sequence.SequenceI;
import msaoperator.io.seqFormat.SequenceFormatInfo;
import msaoperator.io.seqFormat.SequencesWriter;
import msaoperator.io.seqFormat.parser.FastaParser;
import msaoperator.io.seqFormat.writer.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class MS2AlignmentUtil {
	// input or out put
	private File inputFile;

	private BasicSequenceData basicSequenceData;

	public MS2AlignmentUtil(File file) {
		this.inputFile = file;
	}

	public MS2AlignmentUtil() {

	}

	public SequenceDataForAViewer parseData() {
		
		FastaParser fastaParser = new FastaParser(inputFile);
		try {
			fastaParser.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}
		BasicSequenceData seqElements = fastaParser.getSeqElements();

//		BasicSequenceData seqElements = MSFileUtil.parseMSFile2SeqData(inputFile);
//		if (seqElements == null) {
//			return null;
//		}

		return new SequenceDataForAViewer(seqElements.getDataSequences());
	}

	public void exportDataWithSelectedFormat(String path, String defaultSuffix) {

		// 处理 FASTA Continuous 和 Interleaved 格式
		if (SaveFilterFastaContinuous.FORMAT_SUFFIX.equals(defaultSuffix)) {
			File outputFile = new File(path + ".fas");
			writeFastaContinuous(outputFile);
			return;
		}
		if (SaveFilterFastaInterleaved.FORMAT_SUFFIX.equals(defaultSuffix)) {
			File outputFile = new File(path + ".fas");
			writeFastaInterleaved(outputFile, SaveFilterFastaInterleaved.LINE_LENGTH);
			return;
		}

		File outputFile = new File(path + "." + defaultSuffix);
		SequencesWriter writer = null;

		SequenceFormatInfo sFormatInfo = SequenceFormatInfo.FASTA;

		switch (defaultSuffix) {
		case DataFileSuffixNames.CLUSTALW_DEFAULT_SUFFIX:
			writer = new ClustalWriter(outputFile);
			break;
		case DataFileSuffixNames.FASTA_DEFAULT_SUFFIX:
			writer = new FastaWriter(outputFile);
			break;
		case DataFileSuffixNames.GCGMSF_DEFAULT_SUFFIX:
			writer = new GCGMSFWriter(outputFile);
			break;
		case DataFileSuffixNames.MEGA_DEFAULT_SUFFIX:
			writer = new MEGAWriter(outputFile);
			break;
		case DataFileSuffixNames.PAML_DEFAULT_SUFFIX:
			writer = new PAMLWriter(outputFile);
			break;
		case DataFileSuffixNames.PHYLIP_DEFAULT_SUFFIX:
			writer = new PHYLIPWriter(outputFile);
			break;
		case DataFileSuffixNames.NEXUS_DEFAULT_SUFFIX:
			writer = new NEXUSWriter(outputFile);
			break;

		default:
			break;
		}

		writer.setElements(basicSequenceData, true);
		try {
			writer.write(sFormatInfo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 写入 FASTA Continuous 格式 - 序列不换行
	 */
	private void writeFastaContinuous(File file) {
		List<SequenceI> sequences = basicSequenceData.getDataSequences();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			for (SequenceI seq : sequences) {
				bw.write(">");
				bw.write(seq.getSeqName());
				bw.newLine();
				bw.write(seq.getSeqAsString());
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 写入 FASTA Interleaved 格式 - 每 lineLength 个碱基换行
	 */
	private void writeFastaInterleaved(File file, int lineLength) {
		List<SequenceI> sequences = basicSequenceData.getDataSequences();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			for (SequenceI seq : sequences) {
				bw.write(">");
				bw.write(seq.getSeqName());
				bw.newLine();
				String seqStr = seq.getSeqAsString();
				for (int i = 0; i < seqStr.length(); i += lineLength) {
					int end = Math.min(i + lineLength, seqStr.length());
					bw.write(seqStr.substring(i, end));
					bw.newLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setSequenceData(BasicSequenceData data) {
		this.basicSequenceData = data;
	}
	
	public void quickWriteToFasta(List<String> seqNames, List<String> seqs, File file) {
		Iterator<String> iterator = seqNames.iterator();
		Iterator<String> iterator2 = seqs.iterator();
		// 使用try-with-resources自动管理资源
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
			// 写入内容
			while (iterator.hasNext()) {
				String seqName = iterator.next();
				bufferedWriter.write(">");
				bufferedWriter.write(seqName);
				bufferedWriter.write("\n");
				String sequence = iterator2.next();
				bufferedWriter.write(sequence);
				bufferedWriter.write("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
