#!/usr/bin/env python

from pixml.analysis.testing import PluginUnitTestCase, TestAsset
from pixml.analysis import Frame
from pixml_analysis.classifiers.processors import NeuralNetClassifierProcessor


class ClassifiersUnitTests(PluginUnitTestCase):
    @classmethod
    def setUpClass(cls):
        super(ClassifiersUnitTests, cls).setUpClass()

    def setUp(self):
        self.frame = Frame(TestAsset("gs://foo/bar/jpg"))
        self.frame.asset.set_attr(
            'analysis.imageSimilarity.shash',
            '0050FPPOGPPKPIPCAPPPDHHIGPCLJEPLPPPAPPGPDPFPPGPADCBFGIPGDDCIPIGPPDPABPOMPNPAPBPPDKP' +
            'MPJMAPBPDPBOGLPDPPADOENPAAPBPPAPGAIGPDIPOGEPLCGAPPPPOAPBAJFMPPPDCPNKGAAMPLHIPEPDICO' +
            'PLPIAEPACLJPCKMPGBPOLJAABLAPAPMMPOFPKPHEPLBPPMLPKFNPMKACECLPJPPABPPPAFPGIOEPBPAPPOH' +
            'AFAHPPPJPPMDPAPPPELPAPCPPAKMLHJGPAGPMKPALPJPOMPPPPPJPPPAPPPLFINKPEBAAPADDPNMPLPPCBD' +
            'PPOCCBCIPHDACPPCPHBDPBAPBPPALAPHPCFBCADAAPPHPHEPNHNCBOPDPPIPEABNHFPAPEPPDPPAAAPLMDP' +
            'EAKPPPEGPAFPPBABDAOCKPPNPOPPGBGGPPKPMBPCDKCPBCDAPAPACAPPKHPGPPPPANPCEPCPJJGPPPEAPPA' +
            'LPAIPEIICAGPPEPDDPNPFACMPGPBPDCFICPAPPPPCPCPNIGPBPNAGPPJEPEPPAHPAEAMKPBMLAPHPNPPDAK' +
            'PGCPGPPPPPPABABPCPPANPPDEPHGCKFPAPPAPOEHNAAAKPDPNPOAGOPBPPPAPIPPIDDAPPPMAAPEMAIGAKG' +
            'ABPPPGLPAPDOEHKIPPHPNPJCNPAGECLIIFAKPDIGDAPNPDPPIGACAOPACPBPBCAPPKPPIMPFKAALPAPGBAK' +
            'AKPJPKDOAPPBPPPPPHIAPPBPPPPBFPPABACPGDJIPBPLIHPHPLPPIPLAPBCOGOJPGAPAGPDPMAMCNLGLHIL' +
            'PMGDPAMAPJPHPPLPDFBDAKPLPPPKOPPGIFCPDJJPOPAPNAADAGPLGAEPMAFOPEPAHIDPDPBAJMEOGAPCGCA' +
            'MPNCJFDPPGGDCMJPHMDPLGDAMIACPPPPPKPPIGPPPPOACPADPFPKBPNPCHPHBFAPONFKAEAPADPACAMLPPI' +
            'PPDMPCPAPBCPCPJPJFFHPPCPGMPPFHPAMEAOIPPPDPPMAPJAHPPPPPCPAFPAAPEMAAMPPPBBAPDPLHDPPPB' +
            'PPGHEEPDPDPPPEFGPBPBFEPLEPAAPEAPPEMAPPGJLGPPADNPPFMDPMFMPNPAOBMPAAICPGNPPJPPPDOPDBP' +
            'ADAPHPPDAAAEHAPCPIAKLPGAHLPHDBABPHGPKBNFPPGPADPKEFPEEMPDJBIPAPBPCPBHPPCIODDAPLGPBDP' +
            'APKBIPNBCIFHMDPAHPPPPCPPHABAAGPCPAACKFLJPPBAPPMPMOPAACJEPGPBEEHPPGPHPPIAPMPBEBPJBBF' +
            'BPKPPHPPAPDECPPOBIPPIPBAAAPABGABPAPGGJJPAEEPBPABDEBPPODCPGDPKPPPAPPJDBKPOPABPPAIGGG' +
            'PCBDPJBBPPAPPAPADEPPAPAPPFPPPPPPPCDPPLDIDBNPOECGPJPGLPPACPEPPPPPFFAPPBPCAMPKGPAGOPP' +
            'PPGAAFPAPPLPEPHPKBAEJPPBDPPDPPIGOGPFPKBLMBPPAHPIJPLEAIHPPPBFBOEADBPFCDPPGAAPMKPNPCP' +
            'MAPJPHEOPPIKEEOJPKEDJIEAIJPPPBNPEPPJJADFCKMCPADABEABPAPPAOBKDEIPAACPPDBEPPFPDCJPPCP' +
            'CIABPAPHHPCLPMPNPPJAPNALPGKPOPAGPICGPGPAPPCPDPPGAFADLHIPPPOPPKPPAELAMLKACPIPPJBPBPA' +
            'PCCAPHPPJPFPBKBPHGKDAPPPBAPHDAPMBPOABEPPPPHJNPAEPELCPPPPPPCCAJPBPNFGADHPPIKHGGBPAKF' +
            'APODJCPPPPGCPPPPPPKAAAPKBNPPIHOPKPOHPPPAHJABEPKABGPGGDEAMPAAMFELPHILIPAPEHEPPPPPPLC' +
            'APAPPPJPPIPBDGJPCBPKEGCHPJGJPPKPPPFBAPAFDHPFPEDHPLMPBJAPNPBAJBPAJEPJEAGPPFFPJFPLPIP' +
            'JJEPPJPPEMGLKAAPPDCEPPLAGCEIEMAFPABPIPKPPPBAPKOPJAAKPABDFDCFB')

    def test_NeuralNetClassifier_defaults(self):
        processor = self.init_processor(NeuralNetClassifierProcessor(), {"model": "ClassifierTest"})
        processor.process(self.frame)
        self.assertEquals("sunny_right",
                          self.frame.asset.get_attr("analysis")["imageClassify"]["pred0"])
