
import { NativeEventEmitter, NativeModules } from 'react-native';

console.log('Native modules', NativeModules);

const { RNMagtek } = NativeModules;

export const RNMagtekEventsEmitter = new NativeEventEmitter(RNMagtek);

export default RNMagtek;
